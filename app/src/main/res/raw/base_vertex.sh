attribute vec4 aPosition;
attribute vec4 aTexCoord;
uniform mat4 uMatrix;
uniform mat4 uSTMatrix;

varying vec2 textureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    textureCoordinate = (uSTMatrix*aTexCoord).xy;
}