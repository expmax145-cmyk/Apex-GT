package com.example.racing.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Mesh(
    val vertexData: FloatArray, // x, y, z, nx, ny, nz, r, g, b, a (10 floats per vertex)
    val indices: ShortArray
) {
    val vertexCount = vertexData.size / 10
    val indexCount = indices.size

    private var vboId = 0
    private var eboId = 0
    private var vaoId = 0
    private var isLoaded = false

    fun initGL() {
        if (isLoaded) return

        val vaos = IntArray(1)
        val buffers = IntArray(2)

        GLES30.glGenVertexArrays(1, vaos, 0)
        vaoId = vaos[0]

        GLES30.glGenBuffers(2, buffers, 0)
        vboId = buffers[0]
        eboId = buffers[1]

        val vbb = ByteBuffer.allocateDirect(vertexData.size * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val vertexBuffer: FloatBuffer = vbb.asFloatBuffer().apply {
            put(vertexData)
            position(0)
        }

        val ibb = ByteBuffer.allocateDirect(indices.size * 2).apply {
            order(ByteOrder.nativeOrder())
        }
        val indexBuffer: ShortBuffer = ibb.asShortBuffer().apply {
            put(indices)
            position(0)
        }

        GLES30.glBindVertexArray(vaoId)

        // VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertexData.size * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        // EBO
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * 2,
            indexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        val stride = 10 * 4 // 40 bytes per vertex

        // Attribute 0: Position (3 floats)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Normal (3 floats)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

        // Attribute 2: Color (4 floats)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)

        isLoaded = true
    }

    fun draw() {
        if (!isLoaded) {
            initGL()
        }
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            indexCount,
            GLES30.GL_UNSIGNED_SHORT,
            0
        )
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        if (isLoaded) {
            val vaos = intArrayOf(vaoId)
            val buffers = intArrayOf(vboId, eboId)
            GLES30.glDeleteVertexArrays(1, vaos, 0)
            GLES30.glDeleteBuffers(2, buffers, 0)
            isLoaded = false
        }
    }
}
