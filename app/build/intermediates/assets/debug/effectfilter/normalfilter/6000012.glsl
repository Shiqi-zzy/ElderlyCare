varying highp vec2 textureCoordinate;
 
uniform sampler2D inputTexture;
uniform sampler2D inputTexture2;

uniform highp float alpha;
 
void main()
{
    lowp vec3 origin_texel = texture2D(inputTexture, textureCoordinate).rgb;
    
	lowp vec3 texel = origin_texel;
	texel = vec3(dot(vec3(0.3, 0.6, 0.1), texel));
	texel = vec3(texture2D(inputTexture2, vec2(texel.r, 0.16667)).r);

	gl_FragColor = vec4(mix(origin_texel, texel, alpha), 1.0);
}