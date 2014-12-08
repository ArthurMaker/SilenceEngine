#version 330 core

uniform sampler2D tex;

in vec4 vColor;
in vec2 vTexCoords;

layout(location = 0) out vec4 fragColor;

void main()
{
    vec4 texColor = texture(tex, vTexCoords);

    fragColor = vec4(min(texColor.rgb + vColor.rgb, vec3(1.0)), texColor.a * vColor.a);
}
