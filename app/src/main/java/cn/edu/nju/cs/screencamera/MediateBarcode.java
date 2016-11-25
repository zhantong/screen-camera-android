package cn.edu.nju.cs.screencamera;

import java.util.BitSet;

/**
 * Created by zhantong on 2016/11/24.
 */

public class MediateBarcode {
    Districts districts;
    BarcodeConfig config;
    RawImage rawImage;
    PerspectiveTransform transform;

    public MediateBarcode(RawImage rawImage,BarcodeConfig config){
        this.config=config;
        this.rawImage=rawImage;
        districts=new Districts();
        loadConfig(config);

        int[] vertexes=null;
        try {
            vertexes=rawImage.getBarcodeVertexes();
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        int barcodeWidth=districts.get(Districts.BORDER).get(District.RIGHT).endInBlockX();
        int barcodeHeight=districts.get(Districts.BORDER).get(District.DOWN).endInBlockY();
        System.out.println("barcode: "+barcodeWidth+"x"+barcodeHeight);
        transform=PerspectiveTransform.quadrilateralToQuadrilateral(0,0,barcodeWidth,0,barcodeWidth,barcodeHeight,0,barcodeHeight,vertexes[0],vertexes[1],vertexes[2],vertexes[3],vertexes[4],vertexes[5],vertexes[6],vertexes[7]);
    }
    public BitSet getContent(Zone zone){
        return zone.getContent(transform,rawImage);
    }
    private void loadConfig(BarcodeConfig config){
        int MARGIN=Districts.MARGIN;
        int BORDER=Districts.BORDER;
        int PADDING=Districts.PADDING;
        int MAIN_DISTRICT=Districts.MAIN;

        int LEFT=District.LEFT;
        int UP=District.UP;
        int RIGHT=District.RIGHT;
        int DOWN=District.DOWN;
        int LEFT_UP=District.LEFT_UP;
        int RIGHT_UP=District.RIGHT_UP;
        int RIGHT_DOWN=District.RIGHT_DOWN;
        int LEFT_DOWN=District.LEFT_DOWN;
        int MAIN_ZONE=District.MAIN;

        districts.get(BORDER).set(LEFT_UP,new Zone(config.borderLength.get(District.LEFT),
                config.borderLength.get(District.UP),
                0,
                0));
        districts.get(BORDER).set(UP,new Zone(config.paddingLength.get(District.RIGHT)+config.mainWidth+config.paddingLength.get(District.RIGHT),
                config.borderLength.get(District.UP),
                districts.get(BORDER).get(LEFT_UP).endInBlockX(),
                districts.get(BORDER).get(LEFT_UP).startInBlockY()));
        districts.get(BORDER).set(LEFT,new Zone(config.borderLength.get(District.LEFT),
                config.paddingLength.get(District.UP)+config.mainHeight+config.paddingLength.get(District.DOWN),
                districts.get(BORDER).get(LEFT_UP).startInBlockX(),
                districts.get(BORDER).get(LEFT_UP).endInBlockY()));
        districts.get(BORDER).set(LEFT_DOWN,new Zone(config.borderLength.get(District.LEFT),
                config.borderLength.get(District.DOWN),
                districts.get(BORDER).get(LEFT).startInBlockX(),
                districts.get(BORDER).get(LEFT).endInBlockY()));
        districts.get(BORDER).set(DOWN,new Zone(districts.get(BORDER).get(UP).widthInBlock,
                config.borderLength.get(District.DOWN),
                districts.get(BORDER).get(LEFT_DOWN).endInBlockX(),
                districts.get(BORDER).get(LEFT_DOWN).startInBlockY()));
        districts.get(BORDER).set(RIGHT_UP,new Zone(config.borderLength.get(District.RIGHT),
                config.borderLength.get(District.UP),
                districts.get(BORDER).get(UP).endInBlockX(),
                districts.get(BORDER).get(UP).startInBlockY()));
        districts.get(BORDER).set(RIGHT,new Zone(config.borderLength.get(District.RIGHT),
                districts.get(BORDER).get(LEFT).heightInBlock,
                districts.get(BORDER).get(RIGHT_UP).startInBlockX(),
                districts.get(BORDER).get(RIGHT_UP).endInBlockY()));
        districts.get(BORDER).set(RIGHT_DOWN,new Zone(config.borderLength.get(District.RIGHT),
                config.borderLength.get(District.DOWN),
                districts.get(BORDER).get(RIGHT).startInBlockX(),
                districts.get(BORDER).get(RIGHT).endInBlockY()));

        districts.get(PADDING).set(LEFT_UP,new Zone(config.paddingLength.get(District.RIGHT),
                config.paddingLength.get(District.UP),
                districts.get(BORDER).get(LEFT).endInBlockX(),
                districts.get(BORDER).get(UP).endInBlockY()));
        districts.get(PADDING).set(UP,new Zone(config.mainWidth,
                config.paddingLength.get(District.UP),
                districts.get(PADDING).get(LEFT_UP).endInBlockX(),
                districts.get(PADDING).get(LEFT_UP).startInBlockY()));
        districts.get(PADDING).set(LEFT,new Zone(config.paddingLength.get(District.RIGHT),
                config.mainHeight,
                districts.get(PADDING).get(LEFT_UP).startInBlockX(),
                districts.get(PADDING).get(LEFT_UP).endInBlockY()));
        districts.get(PADDING).set(LEFT_DOWN,new Zone(config.paddingLength.get(District.RIGHT),
                config.paddingLength.get(District.DOWN),
                districts.get(PADDING).get(LEFT).startInBlockX(),
                districts.get(PADDING).get(LEFT).endInBlockY()));
        districts.get(PADDING).set(DOWN,new Zone(districts.get(PADDING).get(UP).widthInBlock,
                config.paddingLength.get(District.DOWN),
                districts.get(PADDING).get(LEFT_DOWN).endInBlockX(),
                districts.get(PADDING).get(LEFT_DOWN).startInBlockY()));
        districts.get(PADDING).set(RIGHT_UP,new Zone(config.paddingLength.get(District.RIGHT),
                config.paddingLength.get(District.UP),
                districts.get(PADDING).get(UP).endInBlockX(),
                districts.get(PADDING).get(UP).startInBlockY()));
        districts.get(PADDING).set(RIGHT,new Zone(config.paddingLength.get(District.RIGHT),
                districts.get(PADDING).get(LEFT).heightInBlock,
                districts.get(PADDING).get(RIGHT_UP).startInBlockX(),
                districts.get(PADDING).get(RIGHT_UP).endInBlockY()));
        districts.get(PADDING).set(RIGHT_DOWN,new Zone(config.paddingLength.get(District.RIGHT),
                config.paddingLength.get(District.DOWN),
                districts.get(PADDING).get(RIGHT).startInBlockX(),
                districts.get(PADDING).get(RIGHT).endInBlockY()));
        districts.get(MAIN_DISTRICT).set(MAIN_ZONE,new Zone(config.mainWidth,
                config.mainHeight,
                districts.get(PADDING).get(LEFT).endInBlockX(),
                districts.get(PADDING).get(UP).endInBlockY()));

        int[] parts=new int[]{District.LEFT,District.UP,District.RIGHT,District.DOWN,
                District.LEFT_UP,District.RIGHT_UP,District.RIGHT_DOWN,District.LEFT_DOWN};
        for(int part:parts){
            districts.get(Districts.BORDER).get(part).addBlock(config.borderBlock.get(part));
            districts.get(Districts.PADDING).get(part).addBlock(config.paddingBlock.get(part));
        }
        districts.get(Districts.MAIN).get(District.MAIN).addBlock(config.mainBlock.get(District.MAIN));
    }
}
