public class NDVIIndex implements Index{
    public float calculate(float visPix, float nirPix){

        float outpixel = 0.0f;

        if( visPix + nirPix == 0.0f ){
            outpixel = 0.0f;
        }else{
            outpixel = (nirPix - visPix)/(nirPix + visPix);
            // Cap anything outside of range
            if( outpixel > 1.0f ){
                outpixel = 1.0f;
            }
            if( outpixel < -1.0f ){
                outpixel = -1.0f;
            }
        }

        return outpixel;
    }
}
