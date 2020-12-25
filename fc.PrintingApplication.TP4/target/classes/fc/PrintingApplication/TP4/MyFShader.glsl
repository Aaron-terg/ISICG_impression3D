// version 420

out vec4 out_FragColor;
in vec3 pos;
void main()
{
	float d = length(pos.xy - (gl_FragCoord.xy)/ vec2(800., 600.));
	out_FragColor = vec4(64./255.,0, 0.,1); // red
	
}
