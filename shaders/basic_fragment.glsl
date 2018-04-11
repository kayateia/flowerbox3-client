#version 150 core

uniform float ambientStrength;
uniform sampler2D diffuseTexture;
uniform sampler2D aoTexture;
// uniform sampler2D sdTexture;
uniform vec3 lightColor;
uniform vec3 lightPos;

in vec3 pass_Normal;
in vec2 pass_TextureCoord;
in vec2 pass_AoTextureCoord;
// in vec2 pass_SdTextureCoord;
in vec3 FragPos;

out vec4 out_Color;

void main(void) {
	// ambient
	vec3 ambient = ambientStrength * lightColor;

	// diffuse
	vec3 norm = normalize(pass_Normal);
	vec3 lightDir = normalize(lightPos - FragPos);
	float diff = max(dot(norm, lightDir), 0.0);
	vec3 diffuse = diff * lightColor;

	vec4 rawAo = texture(aoTexture, pass_AoTextureCoord);
	rawAo = vec4(1.0, 1.0, 1.0, 1.0) - rawAo;
	rawAo = 0.8 * rawAo;
	rawAo = vec4(1.0, 1.0, 1.0, 1.0) - rawAo;
	out_Color = vec4(ambient + diffuse, 1.0) * texture(diffuseTexture, pass_TextureCoord) * rawAo; // * texture(sdTexture, pass_SdTextureCoord);
	// out_Color = vec4(ambient + diffuse, 1.0) * texture(aoTexture, pass_AoTextureCoord);
}
