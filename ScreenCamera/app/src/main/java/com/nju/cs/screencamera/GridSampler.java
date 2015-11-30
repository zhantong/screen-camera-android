package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/11/15.
 */
public class GridSampler {
    public Matrix sampleGrid(BiMatrix img,
                              int dimensionX,
                              int dimensionY,
                              float p1ToX, float p1ToY,
                              float p2ToX, float p2ToY,
                              float p3ToX, float p3ToY,
                              float p4ToX, float p4ToY,
                              float p1FromX, float p1FromY,
                              float p2FromX, float p2FromY,
                              float p3FromX, float p3FromY,
                              float p4FromX, float p4FromY){

        PerspectiveTransform transform = PerspectiveTransform.quadrilateralToQuadrilateral(
                p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY,
                p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY);

        return sampleGrid(img, dimensionX, dimensionY, transform);
    }
    public Matrix sampleGrid(BiMatrix img,int dimensionX,int dimensionY,PerspectiveTransform transform){
        Matrix result=new Matrix(dimensionX,dimensionY);
        //int[][] result=new int[dimensionX][dimensionY];
        float[] points=new float[2*dimensionX];
        int max=points.length;
        for(int y=0;y<dimensionY;y++){
            float iValue=(float)y+0.5f;
            for(int x=0;x<max;x+=2){
                points[x]=(float)(x/2)+0.5f;
                points[x+1]=iValue;
            }
            transform.transformPoints(points);
            for(int x=0;x<max;x+=2){
                if(img.pixelEquals((int)points[x],(int)points[x+1],1)){
                    result.set(x/2,y,1);
                }
            }
        }
        return result;
    }
    public String sampleRow(BiMatrix img,
                            int dimensionX,
                            int dimensionY,
                            float p1ToX, float p1ToY,
                            float p2ToX, float p2ToY,
                            float p3ToX, float p3ToY,
                            float p4ToX, float p4ToY,
                            float p1FromX, float p1FromY,
                            float p2FromX, float p2FromY,
                            float p3FromX, float p3FromY,
                            float p4FromX, float p4FromY,int row){
        PerspectiveTransform transform = PerspectiveTransform.quadrilateralToQuadrilateral(
                p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY,
                p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY);
        StringBuilder stringBuilder=new StringBuilder();
        float[] points=new float[2*dimensionX];
        int max=points.length;
        float rowValue=(float)row+0.5f;
        for(int x=0;x<max;x+=2){
            points[x]=(float)(x/2)+0.5f;
            points[x+1]=rowValue;
        }
        transform.transformPoints(points);
        for(int x=0;x<max;x+=2){
            if(img.pixelEquals((int)points[x],(int)points[x+1],1)){
                stringBuilder.append('1');
            }
            else{
                stringBuilder.append('0');
            }
        }
        return stringBuilder.toString();
    }
}
