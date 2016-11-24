package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/24.
 */

public class MediateBarcode {
    Districts districts;
    BarcodeConfig config;

    public MediateBarcode(BarcodeConfig config){
        this.config=config;
        loadConfig(config);
        districts=new Districts();
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
        districts.get(MARGIN).set(LEFT_UP,new Zone(config.marginLength.get(District.LEFT),
                config.marginLength.get(District.UP),
                0,
                0));
        districts.get(MARGIN).set(LEFT,new Zone(config.marginLength.get(District.LEFT),
                config.borderLength.get(District.UP)+config.paddingLength.get(District.UP)+config.mainHeight+config.paddingLength.get(District.DOWN)+config.borderLength.get(District.DOWN),
                districts.get(MARGIN).get(LEFT_UP).startInBlockX(),
                districts.get(MARGIN).get(LEFT_UP).endInBlockY()));
        districts.get(MARGIN).set(UP,new Zone(config.borderLength.get(District.LEFT)+config.paddingLength.get(District.LEFT)+config.mainWidth+config.paddingLength.get(District.RIGHT)+config.borderLength.get(District.RIGHT),
                config.marginLength.get(District.UP),
                districts.get(MARGIN).get(LEFT_UP).endInBlockX(),
                districts.get(MARGIN).get(LEFT_UP).startInBlockY()));
        districts.get(MARGIN).set(LEFT_DOWN,new Zone(config.marginLength.get(District.LEFT),
                config.marginLength.get(District.DOWN),
                districts.get(MARGIN).get(LEFT).startInBlockX(),
                districts.get(MARGIN).get(LEFT).endInBlockY()));
        districts.get(MARGIN).set(DOWN,new Zone(districts.get(MARGIN).get(UP).widthInBlock,
                config.marginLength.get(District.DOWN),
                districts.get(MARGIN).get(LEFT_DOWN).endInBlockX(),
                districts.get(MARGIN).get(LEFT_DOWN).startInBlockY()));
        districts.get(MARGIN).set(RIGHT_UP,new Zone(config.marginLength.get(District.RIGHT),
                config.marginLength.get(District.UP),
                districts.get(MARGIN).get(UP).endInBlockX(),
                districts.get(MARGIN).get(UP).startInBlockY()));
        districts.get(MARGIN).set(RIGHT,new Zone(config.marginLength.get(District.RIGHT),
                districts.get(MARGIN).get(LEFT).heightInBlock,
                districts.get(MARGIN).get(RIGHT_UP).startInBlockX(),
                districts.get(MARGIN).get(RIGHT_UP).endInBlockY()));
        districts.get(MARGIN).set(RIGHT_DOWN,new Zone(config.marginLength.get(District.RIGHT),
                config.marginLength.get(District.DOWN),
                districts.get(MARGIN).get(RIGHT).startInBlockX(),
                districts.get(MARGIN).get(RIGHT).endInBlockY()));

        districts.get(BORDER).set(LEFT_UP,new Zone(config.borderLength.get(District.LEFT),
                config.borderLength.get(District.UP),
                districts.get(MARGIN).get(LEFT).endInBlockX(),
                districts.get(MARGIN).get(UP).endInBlockY()));
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
            districts.get(Districts.MARGIN).get(part).addBlock(config.marginBlock.get(part));
            districts.get(Districts.BORDER).get(part).addBlock(config.borderBlock.get(part));
            districts.get(Districts.PADDING).get(part).addBlock(config.paddingBlock.get(part));
        }
        districts.get(Districts.MAIN).get(District.MAIN).addBlock(config.mainBlock.get(District.MAIN));
    }
}
