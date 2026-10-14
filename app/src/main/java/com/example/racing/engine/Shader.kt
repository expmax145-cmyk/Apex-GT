package com.example.racing.engine

import android.opengl.GLES30
import android.util.Log

class Shader(
    vertexShaderCode: String,
    fragmentShaderCode: String
) {
    var programId: Int = 0
        private set

    private val uniformLocations = mutableMapOf<String, Int>()

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)

        GLES30.glBindAttribLocation(programId, 0, "a_Position")
        GLES30.glBindAttribLocation(programId, 1, "a_Normal")
        GLES30.glBindAttribLocation(programId, 2, "a_Color")

        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Error linking shader program: $log")
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader ($type): $log")
        }
        return shader
    }

    fun use() {
        if (programId != 0) {
            GLES30.glUseProgram(programId)
        }
    }

    private fun getUniformLocation(name: String): Int {
        if (programId == 0) return -1
        return uniformLocations.getOrPut(name) {
            GLES30.glGetUniformLocation(programId, name)
        }
    }

    fun setMat4(name: String, matrix: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc >= 0) {
            GLES30.glUniformMatrix4fv(loc, 1, false, matrix, 0)
        }
    }

    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) {
            GLES30.glUniform3f(loc, x, y, z)
        }
    }

    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) {
            GLES30.glUniform4f(loc, x, y, z, w)
        }
    }

    fun setVec4(name: String, values: FloatArray) {
        val loc = getUniformLocation(name)
        if (loc >= 0) {
            GLES30.glUniform4fv(loc, 1, values, 0)
        }
    }

    fun setFloat(name: String, value: Float) {
        val loc = getUniformLocation(name)
        if (loc >= 0) {
            GLES30.glUniform1f(loc, value)
        }
    }

    fun setInt(name: String, value: Int) {
        val loc = getUniformLocation(name)
        if (loc >= 0) {
            GLES30.glUniform1i(loc, value)
        }
    }

    fun setMaterial(material: Material) {
        setVec4("u_MaterialColor", material.color)
        setVec4("u_SpecularColor", material.specular)
        setFloat("u_Shininess", material.shininess)
        setFloat("u_Metallic", material.metallic)
        setFloat("u_Roughness", material.roughness)
        setFloat("u_Clearcoat", material.clearcoat)
        setVec4("u_EmissionColor", material.emission)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
            uniformLocations.clear()
        }
    }

    companion object {
        val DEFAULT_VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec3 a_Position;
            layout(location = 1) in vec3 a_Normal;
            layout(location = 2) in vec4 a_Color;

            uniform mat4 u_ModelMatrix;
            uniform mat4 u_ViewMatrix;
            uniform mat4 u_ProjectionMatrix;

            out vec3 v_WorldPos;
            out vec3 v_WorldNormal;
            out vec4 v_VertexColor;
            out float v_Distance;

            void main() {
                vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
                v_WorldPos = worldPos.xyz;
                
                mat3 normalMatrix = mat3(u_ModelMatrix);
                v_WorldNormal = normalize(normalMatrix * a_Normal);
                
                v_VertexColor = a_Color;
                
                vec4 viewPos = u_ViewMatrix * worldPos;
                v_Distance = length(viewPos.xyz);
                
                gl_Position = u_ProjectionMatrix * viewPos;
            }
        """.trimIndent()

        val DEFAULT_FRAGMENT_SHADER = """#version 300 es
            precision highp float;

            in vec3 v_WorldPos;
            in vec3 v_WorldNormal;
            in vec4 v_VertexColor;
            in float v_Distance;

            uniform vec3 u_CameraPos;
            uniform vec3 u_SunDirection;
            uniform vec3 u_SunColor;
            uniform vec3 u_SkyAmbient;
            uniform vec3 u_GroundAmbient;

            // Headlight illumination forward from car
            uniform vec3 u_HeadlightPos;
            uniform vec3 u_HeadlightDir;
            uniform vec3 u_HeadlightColor;
            uniform float u_HeadlightIntensity;

            // Material properties
            uniform vec4 u_MaterialColor;
            uniform vec4 u_SpecularColor;
            uniform float u_Shininess;
            uniform float u_Metallic;
            uniform float u_Roughness;
            uniform float u_Clearcoat;
            uniform vec4 u_EmissionColor;

            // Automotive dynamics
            uniform float u_BrakeIntensity;
            uniform float u_WetRoadActive;

            // Atmospheric fog
            uniform vec3 u_FogColor;
            uniform float u_FogNear;
            uniform float u_FogFar;

            // Dynamic Sky and Atmosphere
            uniform vec3 u_SkyZenithColor;
            uniform vec3 u_SkyHorizonColor;
            uniform vec3 u_CloudColor;
            uniform float u_CloudCoverage;
            uniform float u_Time;

            out vec4 fragColor;

            const float PI = 3.14159265359;

            // Cook-Torrance GGX Normal Distribution Function (NDF)
            float distributionGGX(float NdotH, float roughness) {
                float a = roughness * roughness;
                float a2 = a * a;
                float NdotH2 = NdotH * NdotH;
                float num = a2;
                float denom = (NdotH2 * (a2 - 1.0) + 1.0);
                denom = PI * denom * denom;
                return num / max(denom, 0.0001);
            }

            // Smith GGX Geometry Shadowing Masking Term
            float geometrySchlickGGX(float NdotV, float roughness) {
                float r = (roughness + 1.0);
                float k = (r * r) / 8.0;
                float num = NdotV;
                float denom = NdotV * (1.0 - k) + k;
                return num / max(denom, 0.0001);
            }

            float geometrySmith(float NdotV, float NdotL, float roughness) {
                float ggx2 = geometrySchlickGGX(NdotV, roughness);
                float ggx1 = geometrySchlickGGX(NdotL, roughness);
                return ggx1 * ggx2;
            }

            // Fresnel-Schlick with grazing angle reflection
            vec3 fresnelSchlick(float cosTheta, vec3 F0) {
                return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
            }

            // Pseudo-random noise for subtle metallic flake sparkle & asphalt micro-grain
            float hash21(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            void main() {
                vec3 N = normalize(v_WorldNormal);
                vec3 V = normalize(u_CameraPos - v_WorldPos);
                vec3 L = normalize(u_SunDirection);
                vec3 H = normalize(L + V);

                float NdotL = max(dot(N, L), 0.0);
                float NdotV = max(dot(N, V), 0.001);
                float NdotH = max(dot(N, H), 0.0);
                float VdotH = max(dot(V, H), 0.0);

                // --- 1. Base Albedo & Material Flake Texture ---
                vec4 baseColor = u_MaterialColor * v_VertexColor;
                vec3 albedo = baseColor.rgb;

                // Dedicated dynamic path for Panoramic Sky Dome & horizon backdrop
                if (u_Roughness >= 0.99 && u_SpecularColor.r == 0.0 && u_SpecularColor.g == 0.0 && u_SpecularColor.b == 0.0 && u_Clearcoat == 0.0) {
                    vec3 skyDir = normalize(v_WorldPos);
                    float elevation = clamp(skyDir.y, 0.0, 1.0);
                    // Smooth celestial gradient from horizon to zenith
                    vec3 skyBase = mix(u_SkyHorizonColor, u_SkyZenithColor, pow(elevation, 0.65));

                    // Sun disc and atmospheric solar halo
                    vec3 sunDirNorm = normalize(u_SunDirection);
                    float sunDot = max(dot(skyDir, sunDirNorm), 0.0);
                    if (sunDirNorm.y > -0.05) {
                        float sunDisc = pow(sunDot, 480.0) * 2.8;
                        float sunCorona = pow(sunDot, 18.0) * 0.42;
                        skyBase += u_SunColor * (sunDisc + sunCorona);
                    }

                    // Twinkling celestial stars at night (when sun is below horizon)
                    if (sunDirNorm.y < 0.12 && elevation > 0.08) {
                        float starCoord = hash21(floor(skyDir.xz / max(elevation, 0.08) * 140.0));
                        if (starCoord > 0.984 && u_CloudCoverage < 0.75) {
                            float twinkle = 0.65 + 0.35 * sin(u_Time * 3.5 + starCoord * 80.0);
                            float nightFade = clamp((0.12 - sunDirNorm.y) / 0.15, 0.0, 1.0);
                            skyBase += vec3(0.92, 0.95, 1.0) * (starCoord - 0.984) * 55.0 * twinkle * nightFade;
                        }
                    }

                    // Procedural soft cloud coverage drifting across sky dome
                    if (u_CloudCoverage > 0.04) {
                        vec2 cloudUV = (skyDir.xz / max(elevation, 0.12)) * 3.2 + vec2(u_Time * 0.008, u_Time * 0.004);
                        float c1 = hash21(floor(cloudUV * 4.0));
                        float c2 = hash21(floor(cloudUV * 8.0 + vec2(1.7, 3.2)));
                        float cloudNoise = c1 * 0.65 + c2 * 0.35;
                        float cloudDensity = smoothstep(1.0 - u_CloudCoverage * 0.75, 1.0, cloudNoise);
                        skyBase = mix(skyBase, u_CloudColor, cloudDensity * clamp(u_CloudCoverage, 0.0, 0.92));
                    }

                    vec3 x = max(vec3(0.0), skyBase - vec3(0.004));
                    vec3 toneMapped = (x * (6.2 * x + 0.5)) / (x * (6.2 * x + 1.7) + 0.06);
                    fragColor = vec4(toneMapped, baseColor.a);
                    return;
                }

                // Subtle metallic flake sparkle for metallic car paint
                if (u_Metallic > 0.4 && u_Clearcoat > 0.2) {
                    float flakeNoise = hash21(floor(v_WorldPos.xz * 22.0) + floor(v_WorldPos.yy * 22.0));
                    float flakeSparkle = pow(clamp(dot(reflect(-L, N), V), 0.0, 1.0), 16.0) * flakeNoise * 0.18;
                    albedo += albedo * flakeSparkle;
                }

                // --- 2. Ambient Lighting with Sky/Ground Hemisphere & Indirect Fresnel ---
                float skyFactor = clamp(N.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 ambientColor = mix(u_GroundAmbient, u_SkyAmbient, skyFactor);

                // Subdued reflection approximation in ambient
                vec3 R = reflect(-V, N);
                float envSkyFactor = clamp(R.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 envSkyReflection = mix(u_GroundAmbient * 1.2, u_SkyAmbient * 1.4, envSkyFactor);

                // Dielectric vs Metallic base reflectivity
                vec3 F0 = mix(vec3(0.04), u_SpecularColor.rgb, u_Metallic);
                vec3 F_ambient = fresnelSchlick(NdotV, F0);
                vec3 kD_ambient = (vec3(1.0) - F_ambient) * (1.0 - u_Metallic);

                vec3 ambient = kD_ambient * albedo * ambientColor + envSkyReflection * F_ambient * (1.0 - u_Roughness);

                // --- 3. Direct Sun Lighting (Cook-Torrance Specular BRDF) ---
                float r = clamp(u_Roughness, 0.04, 1.0);
                float NDF = distributionGGX(NdotH, r);
                float G = geometrySmith(NdotV, NdotL, r);
                vec3 F = fresnelSchlick(VdotH, F0);

                vec3 numerator = NDF * G * F;
                float denominator = 4.0 * NdotV * NdotL + 0.0001;
                vec3 specularBRDF = numerator / denominator;

                vec3 kS = F;
                vec3 kD = (vec3(1.0) - kS) * (1.0 - u_Metallic);

                vec3 directLight = (kD * albedo / PI + specularBRDF) * u_SunColor * NdotL;

                // --- 4. Secondary Automotive Clearcoat Top Layer ---
                vec3 clearcoatSpecular = vec3(0.0);
                if (u_Clearcoat > 0.05) {
                    // Smooth clearcoat has very low roughness (0.04)
                    float ccNDF = distributionGGX(NdotH, 0.06);
                    float ccG = geometrySmith(NdotV, NdotL, 0.15);
                    vec3 ccF = fresnelSchlick(VdotH, vec3(0.04)); // Clearcoat resin F0 is ~0.04
                    vec3 ccBRDF = (ccNDF * ccG * ccF) / (4.0 * NdotV * NdotL + 0.0001);
                    clearcoatSpecular = ccBRDF * u_SunColor * NdotL * u_Clearcoat;
                }

                // --- 5. Forward Headlight Spotlight Illumination ---
                vec3 headlightLight = vec3(0.0);
                if (u_HeadlightIntensity > 0.01) {
                    vec3 toPoint = v_WorldPos - u_HeadlightPos;
                    float distHL = length(toPoint);
                    if (distHL > 0.1 && distHL < 120.0) {
                        vec3 hlDir = normalize(toPoint);
                        float spotDot = dot(hlDir, normalize(u_HeadlightDir));
                        // Dual cone falloff: inner cone ~22 deg (0.927), outer ~35 deg (0.819)
                        float spotFactor = smoothstep(0.80, 0.94, spotDot);
                        if (spotFactor > 0.0) {
                            float atten = 1.0 / (1.0 + 0.04 * distHL + 0.003 * distHL * distHL);
                            vec3 hlL = -hlDir;
                            float hlNdotL = max(dot(N, hlL), 0.0);
                            vec3 hlDiffuse = albedo * hlNdotL * (1.0 - u_Metallic * 0.8);
                            headlightLight = u_HeadlightColor * (hlDiffuse * spotFactor * atten * u_HeadlightIntensity);
                        }
                    }
                }

                // --- 6. Wet Road Reflection & Anisotropic Horizon Sheen ---
                vec3 wetReflection = vec3(0.0);
                if (u_WetRoadActive > 0.05 && N.y > 0.55) {
                    // Asphalt moisture light absorption: darkens base asphalt without muddying bright painted lines
                    if (u_Roughness > 0.35 && u_Metallic < 0.2) {
                        albedo *= mix(vec3(1.0), vec3(0.68, 0.68, 0.70), u_WetRoadActive * 0.65);
                    }
                    // Strong grazing Fresnel on wet mirror surface
                    float wetF = pow(clamp(1.0 - NdotV, 0.0, 1.0), 3.2);
                    // Puddle micro-surface variation
                    float puddleNoise = hash21(floor(v_WorldPos.xz * 1.5)) * 0.28 + 0.72;
                    vec3 puddleEnv = mix(ambientColor * 1.6, u_SunColor * 1.15, 0.55);
                    wetReflection = puddleEnv * (wetF * 1.65 + 0.22) * u_WetRoadActive * puddleNoise;
                }

                // --- 7. LED Headlight, Taillight & Neon Emissive Glow ---
                vec3 emission = u_EmissionColor.rgb;
                // High-intensity emissive vertices for neon signs, streetlights, and starting lights
                if (v_VertexColor.r > 1.05 || v_VertexColor.g > 1.05 || v_VertexColor.b > 1.05) {
                    emission += max(vec3(0.0), v_VertexColor.rgb - vec3(0.8)) * 2.2;
                }
                if (v_VertexColor.r > 0.85 && v_VertexColor.g < 0.20 && v_VertexColor.b < 0.20 && v_VertexColor.a <= 1.0) {
                    // Constant LED taillight glow ribbon + intense dynamic brake illumination
                    emission += vec3(0.60, 0.04, 0.04) + vec3(3.2 * u_BrakeIntensity, 0.20 * u_BrakeIntensity, 0.20 * u_BrakeIntensity);
                } else if (v_VertexColor.r > 0.92 && v_VertexColor.g > 0.95 && v_VertexColor.b > 0.98 && v_VertexColor.a <= 1.0) {
                    // Crisp Xenon / LED Headlight projector core glow
                    emission += vec3(0.55, 0.65, 0.85);
                }

                // --- 8. Combine Illumination Components ---
                vec3 litColor = ambient + directLight + clearcoatSpecular + headlightLight + wetReflection + emission;

                // --- 9. Atmospheric Distance Fog with Natural Exponential Curve ---
                float fogFactor = clamp((v_Distance - u_FogNear) / max(u_FogFar - u_FogNear, 1.0), 0.0, 1.0);
                fogFactor = smoothstep(0.0, 1.0, fogFactor);
                vec3 withFog = mix(litColor, u_FogColor, fogFactor);

                // --- 10. Filmic Tone Mapping (High-dynamic Range & Contrast) ---
                vec3 x = max(vec3(0.0), withFog - vec3(0.004));
                vec3 toneMapped = (x * (6.2 * x + 0.5)) / (x * (6.2 * x + 1.7) + 0.06);

                fragColor = vec4(toneMapped, baseColor.a);
            }
        """.trimIndent()
    }
}
