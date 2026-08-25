// 红润滤镜
precision mediump float;
varying highp vec2 textureCoordinate;

uniform sampler2D inputTexture; // 图像texture

uniform lowp float power; // 红润程度

void main() {
    lowp vec3 textureColor = texture2D(inputTexture, textureCoordinate).rgb;

    textureColor.r = min(textureColor.r + power * 0.07, 1.0);
    gl_FragColor = vec4(textureColor, 1.0);
}