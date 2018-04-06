#version 150 core

uniform float ambientStrength;
uniform sampler2D texture_diffuse;
uniform vec3 lightColor;
uniform vec3 lightPos;

in vec3 pass_Normal;
in vec2 pass_TextureCoord;
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

	out_Color = vec4(ambient + diffuse, 1.0) * texture(texture_diffuse, pass_TextureCoord);
}
