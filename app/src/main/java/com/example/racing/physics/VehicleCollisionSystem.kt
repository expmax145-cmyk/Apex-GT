package com.example.racing.physics

import com.example.racing.engine.ParticleSystem
import com.example.racing.engine.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance 2D Oriented Bounding Box (OBB) Separating Axis Theorem (SAT)
 * collision detection and impulse-based physical response for racing vehicles.
 *
 * Handles:
 * - Real 2D oriented box collisions (front, rear, side, and angled impacts)
 * - Positional non-penetration separation (mass-weighted)
 * - Normal collision impulse with restitution
 * - Tangential friction impulse (car bodies grinding along each other)
 * - Angular impulse / yaw torque deflection on off-center impacts
 * - Particle spark effects & audio trigger callback
 */
object VehicleCollisionSystem {

    private const val VEHICLE_HALF_WIDTH = 0.98f   // Total body width ~1.96m
    private const val VEHICLE_HALF_LENGTH = 2.15f  // Total body length ~4.30m
    private const val RESTITUTION = 0.32f          // Realistic inelastic automotive bounce
    private const val FRICTION_COEFF = 0.28f       // Coulomb contact friction
    private const val MAX_IMPULSE = 38000f         // Safeguard against physics explosion

    data class ContactManifold(
        val normalX: Float,        // Direction pointing from v2 to v1
        val normalZ: Float,
        val penetration: Float,    // Overlap depth > 0
        val contactX: Float,       // World contact point on XZ plane
        val contactZ: Float
    )

    /**
     * Resolves physical collisions between all pairs of vehicles in the simulation.
     */
    fun resolveVehicleCollisions(
        vehicles: List<VehiclePhysics>,
        playerVehicle: VehiclePhysics,
        particleSystem: ParticleSystem? = null,
        onCollisionImpact: ((Float) -> Unit)? = null
    ) {
        val count = vehicles.size
        for (i in 0 until count) {
            val v1 = vehicles[i]
            for (j in i + 1 until count) {
                val v2 = vehicles[j]

                // Broad-phase: Fast bounding sphere check before OBB SAT
                val dx = v1.position.x - v2.position.x
                val dz = v1.position.z - v2.position.z
                val distSq = dx * dx + dz * dz
                val broadPhaseRadius = (VEHICLE_HALF_LENGTH + 0.3f) * 2f
                if (distSq > broadPhaseRadius * broadPhaseRadius) {
                    continue
                }

                // Narrow-phase: 2D OBB Separating Axis Theorem
                val manifold = testOBBCollision(v1, v2) ?: continue

                // 1. Positional Separation (Prevent deep interpenetration)
                val totalMass = v1.massKg + v2.massKg
                val ratio1 = v2.massKg / totalMass
                val ratio2 = v1.massKg / totalMass
                val separationFactor = 0.65f
                val push1 = manifold.penetration * ratio1 * separationFactor
                val push2 = manifold.penetration * ratio2 * separationFactor

                v1.position.x += manifold.normalX * push1
                v1.position.z += manifold.normalZ * push1
                v2.position.x -= manifold.normalX * push2
                v2.position.z -= manifold.normalZ * push2

                // 2. Compute Relative Velocities at Contact Point
                // Contact vectors from vehicle centers of mass
                val r1x = manifold.contactX - v1.position.x
                val r1z = manifold.contactZ - v1.position.z
                val r2x = manifold.contactX - v2.position.x
                val r2z = manifold.contactZ - v2.position.z

                // Vehicle orientations and directional unit vectors
                val sin1 = sin(v1.headingAngleRad)
                val cos1 = cos(v1.headingAngleRad)
                val sin2 = sin(v2.headingAngleRad)
                val cos2 = cos(v2.headingAngleRad)

                // Linear velocities of centers of mass in world space
                val v1LinX = v1.forwardVelocity * sin1 + v1.lateralVelocity * cos1
                val v1LinZ = v1.forwardVelocity * cos1 - v1.lateralVelocity * sin1
                val v2LinX = v2.forwardVelocity * sin2 + v2.lateralVelocity * cos2
                val v2LinZ = v2.forwardVelocity * cos2 - v2.lateralVelocity * sin2

                // Velocity at contact point including angular velocity (yawRate)
                // v_p = v_lin + (omega * k) x r = (v_lin_x - omega * r_z, v_lin_z + omega * r_x)
                val v1PointX = v1LinX - v1.yawRate * r1z
                val v1PointZ = v1LinZ + v1.yawRate * r1x
                val v2PointX = v2LinX - v2.yawRate * r2z
                val v2PointZ = v2LinZ + v2.yawRate * r2x

                val relVelX = v1PointX - v2PointX
                val relVelZ = v1PointZ - v2PointZ

                val relNormalVel = relVelX * manifold.normalX + relVelZ * manifold.normalZ

                // Only apply impulse if vehicles are moving toward each other
                if (relNormalVel < 0f) {
                    // Moment of inertia for yaw rotation: I_z = mass * (width^2 + length^2) / 12
                    val iz1 = v1.massKg * (VEHICLE_HALF_WIDTH * VEHICLE_HALF_WIDTH + VEHICLE_HALF_LENGTH * VEHICLE_HALF_LENGTH) * 0.45f
                    val iz2 = v2.massKg * (VEHICLE_HALF_WIDTH * VEHICLE_HALF_WIDTH + VEHICLE_HALF_LENGTH * VEHICLE_HALF_LENGTH) * 0.45f

                    // Cross product r x n (in 2D XZ plane: r_x * n_z - r_z * n_x)
                    val r1CrossN = r1x * manifold.normalZ - r1z * manifold.normalX
                    val r2CrossN = r2x * manifold.normalZ - r2z * manifold.normalX

                    // Effective mass along contact normal
                    val invMassTerm = (1f / v1.massKg) + (1f / v2.massKg) +
                            (r1CrossN * r1CrossN / iz1) + (r2CrossN * r2CrossN / iz2)

                    val normalImpulse = (-(1f + RESTITUTION) * relNormalVel / invMassTerm)
                        .coerceIn(0f, MAX_IMPULSE)

                    // Tangential friction impulse (Coulomb model)
                    val tangentX = relVelX - relNormalVel * manifold.normalX
                    val tangentZ = relVelZ - relNormalVel * manifold.normalZ
                    val tangentLen = sqrt(tangentX * tangentX + tangentZ * tangentZ)

                    var frictionImpulseX = 0f
                    var frictionImpulseZ = 0f
                    if (tangentLen > 0.05f) {
                        val tDirX = tangentX / tangentLen
                        val tDirZ = tangentZ / tangentLen

                        val r1CrossT = r1x * tDirZ - r1z * tDirX
                        val r2CrossT = r2x * tDirZ - r2z * tDirX
                        val invMassT = (1f / v1.massKg) + (1f / v2.massKg) +
                                (r1CrossT * r1CrossT / iz1) + (r2CrossT * r2CrossT / iz2)

                        val rawFriction = (-tangentLen / invMassT)
                        val maxFriction = FRICTION_COEFF * normalImpulse
                        val clampedFriction = rawFriction.coerceIn(-maxFriction, maxFriction)

                        frictionImpulseX = tDirX * clampedFriction
                        frictionImpulseZ = tDirZ * clampedFriction
                    }

                    // Total impulse vector on v1 (v2 receives equal and opposite impulse)
                    val totalImpulseX = manifold.normalX * normalImpulse + frictionImpulseX
                    val totalImpulseZ = manifold.normalZ * normalImpulse + frictionImpulseZ

                    // 3. Apply Linear Impulse to Local Velocities
                    val deltaV1X = totalImpulseX / v1.massKg
                    val deltaV1Z = totalImpulseZ / v1.massKg
                    val deltaV2X = -totalImpulseX / v2.massKg
                    val deltaV2Z = -totalImpulseZ / v2.massKg

                    // Project deltaV onto vehicle local forward and lateral axes
                    v1.forwardVelocity += (deltaV1X * sin1 + deltaV1Z * cos1) * 0.95f
                    v1.lateralVelocity += (deltaV1X * cos1 - deltaV1Z * sin1) * 0.85f

                    v2.forwardVelocity += (deltaV2X * sin2 + deltaV2Z * cos2) * 0.95f
                    v2.lateralVelocity += (deltaV2X * cos2 - deltaV2Z * sin2) * 0.85f

                    // 4. Apply Angular Impulse (Yaw Torque Deflection)
                    val torque1 = r1x * totalImpulseZ - r1z * totalImpulseX
                    val torque2 = -(r2x * totalImpulseZ - r2z * totalImpulseX)
                    val maxYawDelta = 1.6f // Safeguard against spinning out of control
                    v1.yawRate += (torque1 / iz1).coerceIn(-maxYawDelta, maxYawDelta)
                    v2.yawRate += (torque2 / iz2).coerceIn(-maxYawDelta, maxYawDelta)

                    // 5. Visual and Audio Feedback
                    val impactSpeed = abs(relNormalVel)
                    if (impactSpeed > 1.2f) {
                        val normalizedImpact = (impactSpeed / 18f).coerceIn(0.35f, 1.5f)
                        onCollisionImpact?.invoke(normalizedImpact)

                        // Spawn collision sparks at contact point
                        val contactElevY = (v1.position.y + v2.position.y) * 0.5f + 0.15f
                        particleSystem?.emitCollisionSparks(
                            manifold.contactX,
                            contactElevY,
                            manifold.contactZ,
                            normalizedImpact
                        )
                    }
                }
            }
        }
    }

    /**
     * 2D Oriented Bounding Box collision test using the Separating Axis Theorem (SAT).
     * Returns contact manifold if overlapping, or null if separated.
     */
    private fun testOBBCollision(v1: VehiclePhysics, v2: VehiclePhysics): ContactManifold? {
        val c1x = v1.position.x
        val c1z = v1.position.z
        val sin1 = sin(v1.headingAngleRad)
        val cos1 = cos(v1.headingAngleRad)
        // Local axes for v1
        val u1x = cos1
        val u1z = -sin1 // Lateral axis
        val v1x = sin1
        val v1z = cos1  // Longitudinal axis

        val c2x = v2.position.x
        val c2z = v2.position.z
        val sin2 = sin(v2.headingAngleRad)
        val cos2 = cos(v2.headingAngleRad)
        // Local axes for v2
        val u2x = cos2
        val u2z = -sin2 // Lateral axis
        val v2x = sin2
        val v2z = cos2  // Longitudinal axis

        // Center-to-center offset vector
        val tX = c1x - c2x
        val tZ = c1z - c2z

        // 4 potential separating axes: u1, v1, u2, v2
        val axes = arrayOf(
            Pair(u1x, u1z),
            Pair(v1x, v1z),
            Pair(u2x, u2z),
            Pair(v2x, v2z)
        )

        var minOverlap = Float.MAX_VALUE
        var bestNormalX = 0f
        var bestNormalZ = 0f

        for (axis in axes) {
            val ax = axis.first
            val az = axis.second

            // Project half-extents of box 1 onto axis
            val r1 = VEHICLE_HALF_WIDTH * abs(u1x * ax + u1z * az) +
                    VEHICLE_HALF_LENGTH * abs(v1x * ax + v1z * az)

            // Project half-extents of box 2 onto axis
            val r2 = VEHICLE_HALF_WIDTH * abs(u2x * ax + u2z * az) +
                    VEHICLE_HALF_LENGTH * abs(v2x * ax + v2z * az)

            // Project distance vector onto axis
            val distProj = abs(tX * ax + tZ * az)

            val overlap = (r1 + r2) - distProj
            if (overlap <= 0f) {
                // Separating axis found: no collision
                return null
            }

            if (overlap < minOverlap) {
                minOverlap = overlap
                // Orient normal to point from v2 to v1
                val sign = if (tX * ax + tZ * az >= 0f) 1f else -1f
                bestNormalX = ax * sign
                bestNormalZ = az * sign
            }
        }

        // Approximate contact point as the midpoint of overlapping region
        val contactX = (c1x + c2x) * 0.5f - bestNormalX * (minOverlap * 0.5f)
        val contactZ = (c1z + c2z) * 0.5f - bestNormalZ * (minOverlap * 0.5f)

        return ContactManifold(
            normalX = bestNormalX,
            normalZ = bestNormalZ,
            penetration = minOverlap,
            contactX = contactX,
            contactZ = contactZ
        )
    }
}
