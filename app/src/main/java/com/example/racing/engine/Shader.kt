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
            precision mediump float;

            in vec3 v_WorldPos;
            in vec3 v_WorldNormal;
            in vec4 v_VertexColor;
            in float v_Distance;

            uniform vec3 u_CameraPos;
            uniform vec3 u_SunDirection;
            uniform vec3 u_SunColor;
            uniform vec3 u_SkyAmbient;
            uniform vec3 u_GroundAmbient;

            uniform vec4 u_MaterialColor;
            uniform vec4 u_SpecularColor;
            uniform float u_Shininess;
            uniform float u_Metallic;
            uniform float u_Roughness;
            uniform float u_Clearcoat;
            uniform vec4 u_EmissionColor;

            uniform float u_BrakeIntensity;
            uniform float u_WetRoadActive;

            uniform vec3 u_FogColor;
            uniform float u_FogNear;
            uniform float u_FogFar;

            out vec4 fragColor;

            void main() {
                vec3 normal = normalize(v_WorldNormal);
                vec3 lightDir = normalize(u_SunDirection);
                vec3 viewDir = normalize(u_CameraPos - v_WorldPos);
                vec3 halfDir = normalize(lightDir + viewDir);

                // Sky / Ground Ambient Hemisphere
                float skyFactor = clamp(normal.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 ambient = mix(u_GroundAmbient, u_SkyAmbient, skyFactor);

                // Energy-conserving diffuse N dot L
                float NdotL = max(dot(normal, lightDir), 0.0);
                float NdotV = max(dot(normal, viewDir), 0.001);
                float NdotH = max(dot(normal, halfDir), 0.0);
                float VdotH = max(dot(viewDir, halfDir), 0.0);

                vec3 diffuse = u_SunColor * NdotL * (1.0 - u_Metallic * 0.85);

                // Cook-Torrance GGX Specular
                float r = clamp(u_Roughness, 0.04, 1.0);
                float a2 = r * r * r * r;
                float dDenom = (NdotH * NdotH * (a2 - 1.0) + 1.0);
                float D = a2 / max(3.14159 * dDenom * dDenom, 0.001);

                // Schlick Fresnel
                vec3 F0 = mix(vec3(0.04), u_SpecularColor.rgb, u_Metallic);
                vec3 F = F0 + (1.0 - F0) * pow(clamp(1.0 - VdotH, 0.0, 1.0), 5.0);

                // Smith Geometry Term
                float k = (r + 1.0) * (r + 1.0) / 8.0;
                float G = (NdotV / (NdotV * (1.0 - k) + k)) * (NdotL / (NdotL * (1.0 - k) + k));

                vec3 specular = (D * F * G) / max(4.0 * NdotV * NdotL, 0.001);
                specular = clamp(specular, 0.0, 3.5) * u_SunColor;

                // Automotive Clearcoat Top Layer
                if (u_Clearcoat > 0.05) {
                    float ccD = pow(NdotH, 120.0);
                    vec3 ccF = vec3(0.04) + (1.0 - 0.04) * pow(clamp(1.0 - VdotH, 0.0, 1.0), 5.0);
                    specular += ccF * ccD * u_Clearcoat * 1.2;
                }

                // Wet Road Anisotropic Sheen
                if (u_WetRoadActive > 0.05 && normal.y > 0.6) {
                    float wetF = pow(clamp(1.0 - NdotV, 0.0, 1.0), 3.0);
                    vec3 wetReflection = mix(ambient, u_SunColor, 0.7) * wetF * u_WetRoadActive * 1.5;
                    specular += wetReflection;
                }

                // Taillight, Brake Light and Headlight Glow
                vec3 emission = u_EmissionColor.rgb;
                if (v_VertexColor.r > 0.85 && v_VertexColor.g < 0.20 && v_VertexColor.b < 0.20) {
                    // Constant LED taillight glow + dynamic bright brake illumination
                    emission += vec3(0.55, 0.04, 0.04) + vec3(2.8 * u_BrakeIntensity, 0.18 * u_BrakeIntensity, 0.18 * u_BrakeIntensity);
                } else if (v_VertexColor.r > 0.92 && v_VertexColor.g > 0.95 && v_VertexColor.b > 0.98) {
                    // Xenon / LED Headlight glow
                    emission += vec3(0.45, 0.52, 0.65);
                }

                // Combine material, vertex color, lighting
                vec4 baseColor = u_MaterialColor * v_VertexColor;
                vec3 litColor = baseColor.rgb * (ambient + diffuse) + specular + emission;

                // Distance Fog with smoothstep
                float fogFactor = clamp((v_Distance - u_FogNear) / max(u_FogFar - u_FogNear, 1.0), 0.0, 1.0);
                fogFactor = smoothstep(0.0, 1.0, fogFactor);
                vec3 withFog = mix(litColor, u_FogColor, fogFactor);

                // ACES-inspired filmic tone mapping & gamma correction
                vec3 x = max(vec3(0.0), withFog - vec3(0.004));
                vec3 toneMapped = (x * (6.2 * x + 0.5)) / (x * (6.2 * x + 1.7) + 0.06);

                fragColor = vec4(toneMapped, baseColor.a);
            }
        """.trimIndent()
    }
}
