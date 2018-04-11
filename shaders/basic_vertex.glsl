#version 150 core

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

in vec3 in_Position;
in vec3 in_Normal;
in vec2 in_TextureCoord;
in vec2 in_AoTextureCoord;
//in vec2 in_SdTextureCoord;

out vec2 pass_AoTextureCoord;
out vec2 pass_TextureCoord;
//out vec2 pass_SdTextureCoord;
out vec3 pass_Normal;
out vec3 FragPos;

void main(void) {
	// Calculated for lighting later.
	FragPos = vec3(modelMatrix * vec4(in_Position, 1.0));

	// Override gl_Position with our new calculated position
	gl_Position = projectionMatrix * viewMatrix * vec4(FragPos, 1.0);

	// Pass this stuff on to the fragment shader.
	// pass_SdTextureCoord = in_SdTextureCoord;
	pass_AoTextureCoord = in_AoTextureCoord;
	pass_TextureCoord = in_TextureCoord;
	pass_Normal = in_Normal;
}
