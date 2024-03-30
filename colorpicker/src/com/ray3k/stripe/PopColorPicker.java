package com.ray3k.stripe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter.DigitsOnlyFilter;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.ray3k.stripe.PopColorPicker.PopColorPickerEvent.Status;
import com.ray3k.tenpatch.TenPatchDrawable;

import java.util.Locale;

public class PopColorPicker extends PopTable {
    private float r, g, b, a, h, s, br;
    private final Color oldColor = new Color();
    private final Color eventColor = new Color();
    private static final Color tempColor = new Color();
    private Image squareModelImage, verticalModelImage, redImage, greenImage, blueImage, hueImage, saturationImage, brightnessImage, alphaImage, swatchNewImage;
    private Stack squareModelCircleStack;
    private Image squareModelCircleImage, verticalModelArrowImage, redArrowImage, greenArrowImage, blueArrowImage, hueArrowImage, saturationArrowImage, brightnessArrowImage, alphaArrowImage;
    private ImageButton redRadio, greenRadio, blueRadio, hueRadio, saturationRadio, brightnessRadio;
    private TextButton hsbTextButton, hslTextButton, swatchesTextButton;
    private Label brightnessLabel;
    private ButtonGroup<ImageButton> radioGroup;
    private TextField redTextField, greenTextField, blueTextField, hueTextField, saturationTextField, brightnessTextField, alphaTextField, hexTextField;
    private static final int SHIFT_AMOUNT = 10;
    private static Preferences colorPreferences;
    private static Preferences settingsPreferences;
    private int customCounter;
    private DragAndDrop dragAndDrop;
    private final PopColorPickerStyle style;
    private static TenPatchDrawable whiteTenPatch;
    private EventListener buttonListener;
    private EventListener textFieldListener;
    private boolean showAlpha = true;

    public PopColorPicker(Color originalColor, Skin skin) {
        this(originalColor, skin.get(PopColorPickerStyle.class));
    }

    public PopColorPicker(Color originalColor, Skin skin, String style) {
        this(originalColor, skin.get(style, PopColorPickerStyle.class));
    }

    public PopColorPicker(Color originalColor, PopColorPickerStyle style) {
        this.style = style;
        setStyle(style);
        createWhiteTenPatch();

        if (colorPreferences == null) colorPreferences = Gdx.app.getPreferences("com.ray3k.PopColorPicker colors");
        if (settingsPreferences == null) settingsPreferences = Gdx.app.getPreferences("com.ray3k.PopColorPicker settings");

        if (originalColor != null) {
            r = originalColor.r;
            g = originalColor.g;
            b = originalColor.b;
            a = originalColor.a;
        } else {
            r = 1f;
            g = 0f;
            b = 0f;
            a = 1f;
        }
        tempColor.set(ColorUtils.rgb2hsb(r, g, b, a));
        h = tempColor.r;
        s = tempColor.g;
        br = tempColor.b;
        this.oldColor.set(originalColor);

        setModal(true);
        setKeepSizedWithinStage(true);
        setKeepCenteredInWindow(true);
        setAutomaticallyResized(false);

        populate();
        addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (getStage().getKeyboardFocus() == hexTextField && event.getTarget() != hexTextField) getStage().setKeyboardFocus(null);
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {

            }
        });
        key(Keys.ESCAPE, new KeyListener() {
            @Override
            public void keyed() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        PopColorPicker.this.hide();
                        PopColorPicker.this.fire(new PopColorPickerEvent(oldColor, Status.cancelled));
                    }
                });
            }
        }).key(Keys.ENTER, new KeyListener() {
            @Override
            public void keyed() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        PopColorPicker.this.hide();
                        eventColor.set(r, g, b, a);
                        PopColorPicker.this.fire(new PopColorPickerEvent(eventColor, Status.picked));
                        PopColorPicker.this.fire(new ChangeEvent());
                    }
                });
            }
        });
    }

    private void populate() {
        if (hsbTextButton == null || hsbTextButton.isChecked() || hslTextButton.isChecked()) populateRGBlayout();
        else populateSwatchLayout();
    }

    private void populateSwatchLayout() {
        clearChildren();
        dragAndDrop = new DragAndDrop();

        Table table = new Table();
        table.setBackground(style.titleBarBackground);
        add(table).growX();

        Label label = new Label("CHOOSE COLOR", style.labelStyle);
        table.add(label).left().expandX();
        label.setTouchable(Touchable.disabled);

        table.defaults().space(5);
        ButtonGroup<TextButton> buttonGroup = new ButtonGroup<TextButton>();
        hsbTextButton = new TextButton("HSB", style.fileTextButtonStyle);
        hsbTextButton.setProgrammaticChangeEvents(false);
        table.add(hsbTextButton);
        buttonGroup.add(hsbTextButton);
        if (buttonListener != null) hsbTextButton.addListener(buttonListener);

        hsbTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                populateRGBlayout();
                pack();
                updateHSB();
                updateColorDisplay();
            }
        });

        hslTextButton = new TextButton("HSL", style.fileTextButtonStyle);
        hslTextButton.setProgrammaticChangeEvents(false);
        table.add(hslTextButton);
        buttonGroup.add(hslTextButton);
        if (buttonListener != null) hslTextButton.addListener(buttonListener);
        hslTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                populateRGBlayout();
                pack();
                hslTextButton.setChecked(true);
                updateHSB();
                updateColorDisplay();
            }
        });

        swatchesTextButton = new TextButton("SWATCHES", style.fileTextButtonStyle);
        swatchesTextButton.setProgrammaticChangeEvents(false);
        table.add(swatchesTextButton);
        buttonGroup.add(swatchesTextButton);
        if (buttonListener != null) swatchesTextButton.addListener(buttonListener);
        swatchesTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });
        swatchesTextButton.setChecked(true);

        row();
        Table bottomTable = new Table();
        bottomTable.setBackground(style.background);
        add(bottomTable).grow();

        table = new Table();
        table.top();
        ScrollPane scrollPane = new ScrollPane(table, style.scrollPaneStyle);
        scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        bottomTable.add(scrollPane).grow();

        label = new Label("Oklab Lightest Dullest", style.labelStyle);
        table.add(label).left().spaceTop(5);

        table.row();
        HorizontalGroup horizontalGroup = new HorizontalGroup();
        horizontalGroup.wrap();
        horizontalGroup.rowAlign(Align.left);
        table.add(horizontalGroup).growX();

        addSwatch("595d5fff", "lightest dullest black", false, horizontalGroup);
        addSwatch("acb0b2ff", "lightest dullest gray", false, horizontalGroup);
        addSwatch("acb0b2ff", "lightest dullest grey", false, horizontalGroup);
        addSwatch("d0d5d7ff", "lightest dullest silver", false, horizontalGroup);
        addSwatch("faffffff", "lightest dullest white", false, horizontalGroup);
        addSwatch("ad7d87ff", "lightest dullest raspberry", false, horizontalGroup);
        addSwatch("ebb0b0ff", "lightest dullest coral", false, horizontalGroup);
        addSwatch("ebb0b0ff", "lightest dullest salmon", false, horizontalGroup);
        addSwatch("d3a19fff", "lightest dullest brick", false, horizontalGroup);
        addSwatch("e9968cff", "lightest dullest red", false, horizontalGroup);
        addSwatch("e4aa97ff", "lightest dullest ember", false, horizontalGroup);
        addSwatch("af9990ff", "lightest dullest brown", false, horizontalGroup);
        addSwatch("98837aff", "lightest dullest chocolate", false, horizontalGroup);
        addSwatch("d6ab93ff", "lightest dullest cinnamon", false, horizontalGroup);
        addSwatch("ecbd9fff", "lightest dullest orange", false, horizontalGroup);
        addSwatch("f4dec5ff", "lightest dullest peach", false, horizontalGroup);
        addSwatch("f4dec5ff", "lightest dullest skin", false, horizontalGroup);
        addSwatch("f0d3a8ff", "lightest dullest apricot", false, horizontalGroup);
        addSwatch("ded4c6ff", "lightest dullest sand", false, horizontalGroup);
        addSwatch("ded4c6ff", "lightest dullest tan", false, horizontalGroup);
        addSwatch("d1c0a1ff", "lightest dullest bronze", false, horizontalGroup);
        addSwatch("f5ebb2ff", "lightest dullest gold", false, horizontalGroup);
        addSwatch("f5ebb2ff", "lightest dullest saffron", false, horizontalGroup);
        addSwatch("faf8d2ff", "lightest dullest butter", false, horizontalGroup);
        addSwatch("f5ffb9ff", "lightest dullest yellow", false, horizontalGroup);
        addSwatch("a5b18cff", "lightest dullest olive", false, horizontalGroup);
        addSwatch("e1efb0ff", "lightest dullest pear", false, horizontalGroup);
        addSwatch("e0ffbbff", "lightest dullest chartreuse", false, horizontalGroup);
        addSwatch("bce4a7ff", "lightest dullest lime", false, horizontalGroup);
        addSwatch("71876eff", "lightest dullest moss", false, horizontalGroup);
        addSwatch("91c18dff", "lightest dullest cactus", false, horizontalGroup);
        addSwatch("8faa94ff", "lightest dullest fern", false, horizontalGroup);
        addSwatch("c6fdcaff", "lightest dullest celery", false, horizontalGroup);
        addSwatch("abfcafff", "lightest dullest green", false, horizontalGroup);
        addSwatch("9ad6a5ff", "lightest dullest jade", false, horizontalGroup);
        addSwatch("d3eee4ff", "lightest dullest sage", false, horizontalGroup);
        addSwatch("c8fdedff", "lightest dullest mint", false, horizontalGroup);
        addSwatch("a4e4e0ff", "lightest dullest turquoise", false, horizontalGroup);
        addSwatch("81adacff", "lightest dullest ocean", false, horizontalGroup);
        addSwatch("81adacff", "lightest dullest teal", false, horizontalGroup);
        addSwatch("b0fefeff", "lightest dullest cyan", false, horizontalGroup);
        addSwatch("99d7e7ff", "lightest dullest azure", false, horizontalGroup);
        addSwatch("99d7e7ff", "lightest dullest sky", false, horizontalGroup);
        addSwatch("8fb4ceff", "lightest dullest denim", false, horizontalGroup);
        addSwatch("6d8fbfff", "lightest dullest cobalt", false, horizontalGroup);
        addSwatch("6d8fbfff", "lightest dullest sapphire", false, horizontalGroup);
        addSwatch("5873a6ff", "lightest dullest navy", false, horizontalGroup);
        addSwatch("5e8ce3ff", "lightest dullest blue", false, horizontalGroup);
        addSwatch("8287d5ff", "lightest dullest indigo", false, horizontalGroup);
        addSwatch("c8c5f7ff", "lightest dullest lavender", false, horizontalGroup);
        addSwatch("ac99e5ff", "lightest dullest violet", false, horizontalGroup);
        addSwatch("ca95ebff", "lightest dullest purple", false, horizontalGroup);
        addSwatch("c0aec9ff", "lightest dullest mauve", false, horizontalGroup);
        addSwatch("c0aec9ff", "lightest dullest puce", false, horizontalGroup);
        addSwatch("c78ed2ff", "lightest dullest plum", false, horizontalGroup);
        addSwatch("e49deeff", "lightest dullest magenta", false, horizontalGroup);
        addSwatch("f1cfefff", "lightest dullest pink", false, horizontalGroup);
        addSwatch("de93b2ff", "lightest dullest rose", false, horizontalGroup);

        table.row();
        label = new Label("Oklab", style.labelStyle);
        table.add(label).left().spaceTop(5);

        table.row();
        horizontalGroup = new HorizontalGroup();
        horizontalGroup.wrap();
        horizontalGroup.rowAlign(Align.left);
        table.add(horizontalGroup).growX();

        addSwatch("000000ff", "black", false, horizontalGroup);
        addSwatch("7d8183ff", "gray", false, horizontalGroup);
        addSwatch("7d8183ff", "grey", false, horizontalGroup);
        addSwatch("b3b8b9ff", "silver", false, horizontalGroup);
        addSwatch("fbffffff", "white", false, horizontalGroup);
        addSwatch("8f1739ff", "raspberry", false, horizontalGroup);
        addSwatch("fc6565ff", "coral", false, horizontalGroup);
        addSwatch("fc6565ff", "salmon", false, horizontalGroup);
        addSwatch("d2564fff", "brick", false, horizontalGroup);
        addSwatch("fe0e13ff", "red", false, horizontalGroup);
        addSwatch("f25d32ff", "ember", false, horizontalGroup);
        addSwatch("8c593fff", "brown", false, horizontalGroup);
        addSwatch("67381cff", "chocolate", false, horizontalGroup);
        addSwatch("d26a22ff", "cinnamon", false, horizontalGroup);
        addSwatch("fd801bff", "orange", false, horizontalGroup);
        addSwatch("fcc182ff", "peach", false, horizontalGroup);
        addSwatch("fcc182ff", "skin", false, horizontalGroup);
        addSwatch("ffa92cff", "apricot", false, horizontalGroup);
        addSwatch("ffa92cff", "sand", false, horizontalGroup);
        addSwatch("cfb692ff", "tan", false, horizontalGroup);
        addSwatch("ca9037ff", "bronze", false, horizontalGroup);
        addSwatch("f9d821ff", "gold", false, horizontalGroup);
        addSwatch("f9d821ff", "saffron", false, horizontalGroup);
        addSwatch("fef48cff", "butter", false, horizontalGroup);
        addSwatch("fbff25ff", "yellow", false, horizontalGroup);
        addSwatch("7c8212ff", "olive", false, horizontalGroup);
        addSwatch("cfe531ff", "pear", false, horizontalGroup);
        addSwatch("c2ff47ff", "chartreuse", false, horizontalGroup);
        addSwatch("8ed41eff", "lime", false, horizontalGroup);
        addSwatch("1d4609ff", "moss", false, horizontalGroup);
        addSwatch("27a011ff", "cactus", false, horizontalGroup);
        addSwatch("4b7946ff", "fern", false, horizontalGroup);
        addSwatch("7bff75ff", "celery", false, horizontalGroup);
        addSwatch("00ff17ff", "green", false, horizontalGroup);
        addSwatch("2dc147ff", "jade", false, horizontalGroup);
        addSwatch("a7e5c6ff", "sage", false, horizontalGroup);
        addSwatch("7dffd5ff", "mint", false, horizontalGroup);
        addSwatch("12d8cbff", "turquoise", false, horizontalGroup);
        addSwatch("007f81ff", "ocean", false, horizontalGroup);
        addSwatch("007f81ff", "teal", false, horizontalGroup);
        addSwatch("00ffffff", "cyan", false, horizontalGroup);
        addSwatch("00c1e2ff", "azure", false, horizontalGroup);
        addSwatch("00c1e2ff", "sky", false, horizontalGroup);
        addSwatch("2c89baff", "denim", false, horizontalGroup);
        addSwatch("0045afff", "cobalt", false, horizontalGroup);
        addSwatch("0045afff", "sapphire", false, horizontalGroup);
        addSwatch("000081ff", "navy", false, horizontalGroup);
        addSwatch("0000ffff", "blue", false, horizontalGroup);
        addSwatch("5008e3ff", "indigo", false, horizontalGroup);
        addSwatch("b593ffff", "lavender", false, horizontalGroup);
        addSwatch("8e40f1ff", "violet", false, horizontalGroup);
        addSwatch("bf00ffff", "purple", false, horizontalGroup);
        addSwatch("aa73aeff", "mauve", false, horizontalGroup);
        addSwatch("aa73aeff", "puce", false, horizontalGroup);
        addSwatch("bd04c9ff", "plum", false, horizontalGroup);
        addSwatch("f311f8ff", "magenta", false, horizontalGroup);
        addSwatch("fca1e5ff", "pink", false, horizontalGroup);
        addSwatch("e3237dff", "rose", false, horizontalGroup);

        table.row();
        label = new Label("Oklab Darkest", style.labelStyle);
        table.add(label).left().spaceTop(5);

        table.row();
        horizontalGroup = new HorizontalGroup();
        horizontalGroup.wrap();
        horizontalGroup.rowAlign(Align.left);
        table.add(horizontalGroup).growX();

        addSwatch("000000ff", "darkest black", false, horizontalGroup);
        addSwatch("444849ff", "darkest gray", false, horizontalGroup);
        addSwatch("444849ff", "darkest grey", false, horizontalGroup);
        addSwatch("5e6264ff", "darkest silver", false, horizontalGroup);
        addSwatch("85898bff", "darkest white", false, horizontalGroup);
        addSwatch("570020ff", "darkest raspberry", false, horizontalGroup);
        addSwatch("9f1b26ff", "darkest coral", false, horizontalGroup);
        addSwatch("9f1b26ff", "darkest salmon", false, horizontalGroup);
        addSwatch("871a1cff", "darkest brick", false, horizontalGroup);
        addSwatch("8f0009ff", "darkest red", false, horizontalGroup);
        addSwatch("912600ff", "darkest ember", false, horizontalGroup);
        addSwatch("572d17ff", "darkest brown", false, horizontalGroup);
        addSwatch("411c01ff", "darkest chocolate", false, horizontalGroup);
        addSwatch("783700ff", "darkest cinnamon", false, horizontalGroup);
        addSwatch("8e4200ff", "darkest orange", false, horizontalGroup);
        addSwatch("91612bff", "darkest peach", false, horizontalGroup);
        addSwatch("91612bff", "darkest skin", false, horizontalGroup);
        addSwatch("8e5700ff", "darkest apricot", false, horizontalGroup);
        addSwatch("746042ff", "darkest sand", false, horizontalGroup);
        addSwatch("746042ff", "darkest tan", false, horizontalGroup);
        addSwatch("734c00ff", "darkest bronze", false, horizontalGroup);
        addSwatch("857000ff", "darkest gold", false, horizontalGroup);
        addSwatch("857000ff", "darkest saffron", false, horizontalGroup);
        addSwatch("887e22ff", "darkest butter", false, horizontalGroup);
        addSwatch("848500ff", "darkest yellow", false, horizontalGroup);
        addSwatch("424900ff", "darkest olive", false, horizontalGroup);
        addSwatch("6f7800ff", "darkest pear", false, horizontalGroup);
        addSwatch("618900ff", "darkest chartreuse", false, horizontalGroup);
        addSwatch("487300ff", "darkest lime", false, horizontalGroup);
        addSwatch("0e2800ff", "darkest moss", false, horizontalGroup);
        addSwatch("0e5900ff", "darkest cactus", false, horizontalGroup);
        addSwatch("1f471bff", "darkest fern", false, horizontalGroup);
        addSwatch("008f04ff", "darkest celery", false, horizontalGroup);
        addSwatch("008711ff", "darkest green", false, horizontalGroup);
        addSwatch("006b20ff", "darkest jade", false, horizontalGroup);
        addSwatch("487c64ff", "darkest sage", false, horizontalGroup);
        addSwatch("008d6cff", "darkest mint", false, horizontalGroup);
        addSwatch("00746bff", "darkest turquoise", false, horizontalGroup);
        addSwatch("004748ff", "darkest ocean", false, horizontalGroup);
        addSwatch("004748ff", "darkest teal", false, horizontalGroup);
        addSwatch("008687ff", "darkest cyan", false, horizontalGroup);
        addSwatch("00697bff", "darkest azure", false, horizontalGroup);
        addSwatch("00697bff", "darkest sky", false, horizontalGroup);
        addSwatch("004c72ff", "darkest denim", false, horizontalGroup);
        addSwatch("00236eff", "darkest cobalt", false, horizontalGroup);
        addSwatch("00236eff", "darkest sapphire", false, horizontalGroup);
        addSwatch("000149ff", "darkest navy", false, horizontalGroup);
        addSwatch("000091ff", "darkest blue", false, horizontalGroup);
        addSwatch("2c0483ff", "darkest indigo", false, horizontalGroup);
        addSwatch("6646a4ff", "darkest lavender", false, horizontalGroup);
        addSwatch("53009cff", "darkest violet", false, horizontalGroup);
        addSwatch("690090ff", "darkest purple", false, horizontalGroup);
        addSwatch("643768ff", "darkest mauve", false, horizontalGroup);
        addSwatch("643768ff", "darkest puce", false, horizontalGroup);
        addSwatch("690070ff", "darkest plum", false, horizontalGroup);
        addSwatch("870087ff", "darkest magenta", false, horizontalGroup);
        addSwatch("934983ff", "darkest pink", false, horizontalGroup);
        addSwatch("840047ff", "darkest rose", false, horizontalGroup);

        table.row();
        label = new Label("Custom", style.labelStyle);
        table.add(label).left().spaceTop(5);

        table.row();
        final HorizontalGroup customHorizontalGroup = new HorizontalGroup();
        customHorizontalGroup.wrap();
        customHorizontalGroup.rowAlign(Align.left);
        table.add(customHorizontalGroup).growX();

        customCounter = settingsPreferences.getInteger("customCounter", 0);
        for (String name : settingsPreferences.getString("sortOrder", "").split("\\|")) {
            String hex = colorPreferences.getString(name);
            if (hex != null && !hex.equals("")) addSwatch(hex, name, true, customHorizontalGroup);
        }

        final Image newSwatchImage = new Image(style.colorSwatchNew);
        customHorizontalGroup.addActor(newSwatchImage);

        if (buttonListener != null) newSwatchImage.addListener(buttonListener);
        newSwatchImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                customHorizontalGroup.removeActor(newSwatchImage);
                String name = "custom" + customCounter++;
                settingsPreferences.putInteger("customCounter", customCounter);
                settingsPreferences.putString("sortOrder",settingsPreferences.getString("sortOrder", "") + "|" + name);
                settingsPreferences.flush();
                Color color = new Color(r, g, b, a);
                addSwatch(color, name, true, customHorizontalGroup);
                customHorizontalGroup.addActor(newSwatchImage);
                colorPreferences.putString(name, color.toString());
                colorPreferences.flush();
            }
        });
        dragAndDrop.addTarget(new Target(newSwatchImage) {
            @Override
            public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
                return true;
            }

            @Override
            public void drop(Source source, Payload payload, float x, float y, int pointer) {
                Actor sourceActor = source.getActor();
                sourceActor.setVisible(true);
                sourceActor.remove();
                customHorizontalGroup.addActorAt(customHorizontalGroup.getChildren().indexOf(newSwatchImage, true), sourceActor);
                customHorizontalGroup.layout();

                StringBuilder sortOrder = new StringBuilder();
                for (Actor actor : customHorizontalGroup.getChildren()) {
                    Image currentImage = (Image) actor;
                    String name = (String) currentImage.getUserObject();
                    if (name != null) {
                        sortOrder.append("|");
                        sortOrder.append(name);
                    }
                }
                settingsPreferences.putString("sortOrder", sortOrder.toString());
                settingsPreferences.flush();
            }
        });

        bottomTable.row();
        table = new Table();
        table.right();
        bottomTable.add(table).growX();

        table.defaults().space(5);
        Stack stack = new Stack();
        table.add(stack);

        if (oldColor != null) {
            stack.setTouchable(Touchable.enabled);
            if (buttonListener != null) stack.addListener(buttonListener);

            Image image = new Image(style.previewSwatchBackground);
            image.setScaling(Scaling.none);
            stack.add(image);

            swatchNewImage = new Image(style.previewSwatchNew);
            swatchNewImage.setScaling(Scaling.none);
            stack.add(swatchNewImage);

            image = new Image(style.previewSwatchOld);
            image.setColor(oldColor);
            image.setScaling(Scaling.none);
            stack.add(image);
        } else {
            Image image = new Image(style.previewSwatchSingleBackground);
            image.setScaling(Scaling.none);
            stack.add(image);

            swatchNewImage = new Image(style.previewSwatchSingle);
            swatchNewImage.setScaling(Scaling.none);
            stack.add(swatchNewImage);
        }
        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (oldColor != null) {
                    System.out.println(x + " " + y);
                    r = oldColor.r;
                    g = oldColor.g;
                    b = oldColor.b;
                    a = oldColor.a;
                    updateHSB();
                    updateColorDisplay();
                }
            }
        });

        hexTextField = new TextField("", style.hexTextFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        hexTextField.setProgrammaticChangeEvents(false);
        hexTextField.setMaxLength(8);
        hexTextField.setTextFieldFilter(new TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                if (Character.isDigit(c)) return true;
                else switch (c) {
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                        return true;
                    default:
                        return false;
                }
            }
        });
        table.add(hexTextField).width(80);
        if (textFieldListener != null) hexTextField.addListener(textFieldListener);
        applyFieldListener(hexTextField);
        hexTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text = hexTextField.getText();
                if (text.length() == 6) {
                    text += "FF";
                    Color color = Color.valueOf(text);
                    r = color.r;
                    g = color.g;
                    b = color.b;
                    a = 1;
                    updateHSB();
                    updateColorDisplay();
                } else if (text.length() == 8) {
                    Color color = Color.valueOf(text);
                    r = color.r;
                    g = color.g;
                    b = color.b;
                    a = color.a;
                    updateHSB();
                    updateColorDisplay();
                }
            }
        });

        TextButton textButton = new TextButton("OK", style.textButtonStyle);
        table.add(textButton).width(70);
        if (buttonListener != null) textButton.addListener(buttonListener);
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                eventColor.set(r, g, b, a);
                fire(new PopColorPickerEvent(eventColor, Status.picked));
                fire(new ChangeEvent());
            }
        });

        textButton = new TextButton("Cancel", style.textButtonStyle);
        table.add(textButton).width(70);
        if (buttonListener != null) textButton.addListener(buttonListener);
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                fire(new PopColorPickerEvent(oldColor, Status.cancelled));
            }
        });
    }

    private void addSwatch(String hex, String name, boolean custom, HorizontalGroup horizontalGroup) {
        addSwatch(Color.valueOf(hex), name, custom, horizontalGroup);
    }

    private void addSwatch(final Color color, String name, boolean custom, final HorizontalGroup horizontalGroup) {
        final Image image = new Image(style.colorSwatch);
        image.setColor(color);
        horizontalGroup.addActor(image);
        if (buttonListener != null) image.addListener(buttonListener);
        image.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                r = color.r;
                g = color.g;
                b = color.b;
                a = color.a;
                updateHSB();
                updateColorDisplay();
            }
        });

        if (custom) {
            image.setUserObject(name);
            dragAndDrop.addSource(new Source(image) {
                @Override
                public Payload dragStart(InputEvent event, float x, float y, int pointer) {
                    Payload payload = new Payload();
                    image.setVisible(false);
                    Image dragActor = new Image(style.colorSwatch);
                    dragActor.setColor(getActor().getColor());
                    payload.setDragActor(dragActor);
                    getActor().setPosition(0,0);
                    return payload;
                }

                @Override
                public void dragStop(InputEvent event, float x, float y, int pointer, Payload payload, Target target) {
                    if (target == null) {
                        image.remove();
                        colorPreferences.remove((String) image.getUserObject());
                        colorPreferences.flush();

                        StringBuilder sortOrder = new StringBuilder();
                        for (Actor actor : horizontalGroup.getChildren()) {
                            Actor currentImage = actor;
                            String name = (String) currentImage.getUserObject();
                            if (name != null) {
                                sortOrder.append("|");
                                sortOrder.append(name);
                            }
                        }
                        settingsPreferences.putString("sortOrder", sortOrder.toString());
                        settingsPreferences.flush();
                    }
                }
            });
            dragAndDrop.addTarget(new Target(image) {
                @Override
                public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
                    return true;
                }

                @Override
                public void drop(Source source, Payload payload, float x, float y, int pointer) {
                    Actor sourceActor = source.getActor();
                    sourceActor.setVisible(true);
                    sourceActor.remove();
                    horizontalGroup.addActorAt(horizontalGroup.getChildren().indexOf(image, true), sourceActor);
                    horizontalGroup.layout();

                    StringBuilder sortOrder = new StringBuilder();
                    for (Actor actor : horizontalGroup.getChildren()) {
                        Actor currentImage = actor;
                        String name = (String) currentImage.getUserObject();
                        if (name != null) {
                            sortOrder.append("|");
                            sortOrder.append(name);
                        }
                    }
                    settingsPreferences.putString("sortOrder", sortOrder.toString());
                    settingsPreferences.flush();
                }
            });
        }

        PopTableStyle previewPopStyle = new PopTableStyle();
        previewPopStyle.background = style.colorSwatchPopBackground;
        PopTableHoverListener listener = new PopTableHoverListener(Align.topLeft, Align.topRight, previewPopStyle);
        image.addListener(listener);
        PopTable pop = listener.getPopTable();
        pop.pad(5);

        pop.defaults().space(5);
        Image preview = new Image(style.colorSwatchPopPreview);
        preview.setColor(color);
        pop.add(preview).size(40);

        Table table = new Table();
        pop.add(table);

        table.defaults().space(5);
        Label label = new Label(name, style.labelStyle);
        table.add(label);

        table.row();
        label = new Label("r:" + MathUtils.round(color.r * 255) + " g:" + MathUtils.round(color.g * 255) + " b:" + MathUtils.round(color.b * 255), style.labelStyle);
        table.add(label);

        table.row();
        label = new Label("#" + color, style.labelStyle);
        table.add(label);
    }

    private void populateRGBlayout() {
        clearChildren();

        Table table = new Table();
        table.setBackground(style.titleBarBackground);
        add(table).growX();

        Label label = new Label("CHOOSE COLOR", style.labelStyle);
        table.add(label).left().expandX();
        label.setTouchable(Touchable.disabled);

        table.defaults().space(5);
        ButtonGroup<TextButton> buttonGroup = new ButtonGroup<TextButton>();
        hsbTextButton = new TextButton("HSB", style.fileTextButtonStyle);
        hsbTextButton.setProgrammaticChangeEvents(false);
        table.add(hsbTextButton);
        buttonGroup.add(hsbTextButton);
        if (buttonListener != null) hsbTextButton.addListener(buttonListener);
        hsbTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateHSB();
                updateColorDisplay();
            }
        });

        hslTextButton = new TextButton("HSL", style.fileTextButtonStyle);
        hslTextButton.setProgrammaticChangeEvents(false);
        table.add(hslTextButton);
        buttonGroup.add(hslTextButton);
        if (buttonListener != null) hslTextButton.addListener(buttonListener);
        hslTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateHSB();
                updateColorDisplay();
            }
        });

        swatchesTextButton = new TextButton("SWATCHES", style.fileTextButtonStyle);
        swatchesTextButton.setProgrammaticChangeEvents(false);
        table.add(swatchesTextButton);
        buttonGroup.add(swatchesTextButton);
        if (buttonListener != null) swatchesTextButton.addListener(buttonListener);
        swatchesTextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                populateSwatchLayout();
                updateColorDisplay();
            }
        });

        row();
        Table subTable = new Table();
        subTable.setBackground(style.background);
        add(subTable);

        subTable.defaults().space(5);
        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        subTable.add(table).size(250);

        squareModelImage = new Image();
        table.add(squareModelImage).grow();
        if (buttonListener != null) squareModelImage.addListener(buttonListener);
        squareModelImage.addListener(new SquareModelDragListener());

        squareModelCircleStack = new Stack();
        table.addActor(squareModelCircleStack);

        Image image = new Image(style.colorKnobCircleBackground);
        image.setTouchable(Touchable.disabled);
        squareModelCircleStack.add(image);

        squareModelCircleImage = new Image(style.colorKnobCircleForeground);
        squareModelCircleImage.setTouchable(Touchable.disabled);
        squareModelCircleStack.add(squareModelCircleImage);

        squareModelCircleStack.pack();

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        subTable.add(table).size(30, 250);

        verticalModelImage = new Image();
        table.add(verticalModelImage).grow();
        if (buttonListener != null) verticalModelImage.addListener(buttonListener);
        verticalModelImage.addListener(new VerticalModelDragListener());

        verticalModelArrowImage = new Image(style.colorSliderKnobVertical);
        verticalModelArrowImage.setTouchable(Touchable.disabled);
        table.addActor(verticalModelArrowImage);

        Table controlTable = new Table();
        subTable.add(controlTable).growY();

        Table sliderTable = new Table();
        controlTable.add(sliderTable).expandY();

        radioGroup = new ButtonGroup<ImageButton>();

        sliderTable.defaults().space(2);
        redRadio = new ImageButton(style.radioButtonStyle);
        redRadio.setProgrammaticChangeEvents(false);
        sliderTable.add(redRadio);
        radioGroup.add(redRadio);
        if (buttonListener != null) redRadio.addListener(buttonListener);
        redRadio.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });

        label = new Label("R", style.labelStyle);
        sliderTable.add(label);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        sliderTable.add(table).width(180).fillY();

        redImage = new Image();
        redImage.setScaling(Scaling.stretch);
        table.add(redImage).grow();
        if (buttonListener != null) redImage.addListener(buttonListener);
        redImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                r = value;
                PopColorPicker.this.updateHSB();
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        redArrowImage = new Image(style.colorSliderKnobHorizontal);
        redArrowImage.setTouchable(Touchable.disabled);
        table.addActor(redArrowImage);

        redTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        redTextField.setProgrammaticChangeEvents(false);
        redTextField.setTextFieldFilter(new DigitsOnlyFilter());
        sliderTable.add(redTextField).width(50);
        if (textFieldListener != null) redTextField.addListener(textFieldListener);
        applyFieldListener(redTextField);
        redTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r = Integer.parseInt(redTextField.getText()) / 255f;
                r = MathUtils.clamp(r, 0, 1);
                updateHSB();
                updateColorDisplay();
            }
        });

        table = new Table();
        sliderTable.add(table);

        table.defaults().space(3);
        ImageButton imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r += (isShifting() ? SHIFT_AMOUNT : 1) / 255f;
                if (r > 1) r = 1;
                updateHSB();
                updateColorDisplay();
            }
        });

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r -= (isShifting() ? SHIFT_AMOUNT : 1) / 255f;
                if (r < 0) r = 0;
                updateHSB();
                updateColorDisplay();
            }
        });

        sliderTable.row();
        greenRadio = new ImageButton(style.radioButtonStyle);
        greenRadio.setProgrammaticChangeEvents(false);
        sliderTable.add(greenRadio);
        radioGroup.add(greenRadio);
        if (buttonListener != null) greenRadio.addListener(buttonListener);
        greenRadio.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });

        label = new Label("G", style.labelStyle);
        sliderTable.add(label);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        sliderTable.add(table).width(180).fillY();

        greenImage = new Image();
        greenImage.setScaling(Scaling.stretch);
        table.add(greenImage).grow();
        if (buttonListener != null) greenImage.addListener(buttonListener);
        greenImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                g = value;
                PopColorPicker.this.updateHSB();
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        greenArrowImage = new Image(style.colorSliderKnobHorizontal);
        greenArrowImage.setTouchable(Touchable.disabled);
        table.addActor(greenArrowImage);

        greenTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        greenTextField.setProgrammaticChangeEvents(false);
        sliderTable.add(greenTextField).width(50);
        if (textFieldListener != null) greenTextField.addListener(textFieldListener);
        applyFieldListener(greenTextField);
        greenTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                g = Integer.parseInt(greenTextField.getText()) / 255f;
                g = MathUtils.clamp(g, 0, 1);
                updateHSB();
                updateColorDisplay();
            }
        });

        table = new Table();
        sliderTable.add(table);

        table.defaults().space(3);
        imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                g += (isShifting() ? SHIFT_AMOUNT : 1) / 255f;
                if (g > 1) g = 1;
                updateHSB();
                updateColorDisplay();
            }
        });

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                g -= (isShifting() ? SHIFT_AMOUNT : 1) / 255f;
                if (g < 0) g = 0;
                updateHSB();
                updateColorDisplay();
            }
        });

        sliderTable.row();
        blueRadio = new ImageButton(style.radioButtonStyle);
        blueRadio.setProgrammaticChangeEvents(false);
        sliderTable.add(blueRadio);
        radioGroup.add(blueRadio);
        if (buttonListener != null) blueRadio.addListener(buttonListener);
        blueRadio.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });

        label = new Label("B", style.labelStyle);
        sliderTable.add(label);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        sliderTable.add(table).width(180).fillY();

        blueImage = new Image();
        blueImage.setScaling(Scaling.stretch);
        table.add(blueImage).grow();
        if (buttonListener != null) blueImage.addListener(buttonListener);
        blueImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                b = value;
                PopColorPicker.this.updateHSB();
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        blueArrowImage = new Image(style.colorSliderKnobHorizontal);
        blueArrowImage.setTouchable(Touchable.disabled);
        table.addActor(blueArrowImage);

        blueTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        blueTextField.setProgrammaticChangeEvents(false);
        sliderTable.add(blueTextField).width(50);
        if (textFieldListener != null) blueTextField.addListener(textFieldListener);
        applyFieldListener(blueTextField);
        blueTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                b = Integer.parseInt(blueTextField.getText()) / 255f;
                b = MathUtils.clamp(b, 0, 1);
                updateHSB();
                updateColorDisplay();
            }
        });

        table = new Table();
        sliderTable.add(table);

        table.defaults().space(3);
        imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                b += (isShifting() ? SHIFT_AMOUNT : 1) / 255f;
                if (b > 1) b = 1;
                updateHSB();
                updateColorDisplay();
            }
        });

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                b -= (isShifting() ? SHIFT_AMOUNT : 1) / 255f;
                if (b < 0) b = 0;
                updateHSB();
                updateColorDisplay();
            }
        });

        sliderTable.row();
        hueRadio = new ImageButton(style.radioButtonStyle);
        hueRadio.setProgrammaticChangeEvents(false);
        sliderTable.add(hueRadio);
        radioGroup.add(hueRadio);
        hueRadio.setChecked(true);
        if (buttonListener != null) hueRadio.addListener(buttonListener);
        hueRadio.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });

        label = new Label("H", style.labelStyle);
        sliderTable.add(label);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        sliderTable.add(table).width(180).fillY();

        hueImage = new Image(generateHorizontalHue(180));
        table.add(hueImage).grow();
        if (buttonListener != null) hueImage.addListener(buttonListener);
        hueImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                h = value;
                PopColorPicker.this.updateRGB();
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        hueArrowImage = new Image(style.colorSliderKnobHorizontal);
        hueArrowImage.setTouchable(Touchable.disabled);
        table.addActor(hueArrowImage);

        hueTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        hueTextField.setProgrammaticChangeEvents(false);
        sliderTable.add(hueTextField).width(50);
        if (textFieldListener != null) hueTextField.addListener(textFieldListener);
        applyFieldListener(hueTextField);
        hueTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                h = Integer.parseInt(hueTextField.getText()) / 360f;
                h = MathUtils.clamp(h, 0, 1);
                updateRGB();
                updateColorDisplay();
            }
        });

        table = new Table();
        sliderTable.add(table);

        table.defaults().space(3);
        imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                h += (isShifting() ? SHIFT_AMOUNT : 1) / 360f;
                if (h > 1) h = 1;
                updateRGB();
                updateColorDisplay();
            }
        });

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                h -= (isShifting() ? SHIFT_AMOUNT : 1) / 360f;
                if (h < 0) h = 0;
                Color newColor = new Color(ColorUtils.hsb2rgb(h, s, br, a));
                r = newColor.r;
                g = newColor.g;
                b = newColor.b;
                updateColorDisplay();
            }
        });

        sliderTable.row();
        saturationRadio = new ImageButton(style.radioButtonStyle);
        saturationRadio.setProgrammaticChangeEvents(false);
        sliderTable.add(saturationRadio);
        radioGroup.add(saturationRadio);
        if (buttonListener != null) saturationRadio.addListener(buttonListener);
        saturationRadio.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });

        label = new Label("S", style.labelStyle);
        sliderTable.add(label);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        sliderTable.add(table).width(180).fillY();

        saturationImage = new Image();
        saturationImage.setScaling(Scaling.stretch);
        table.add(saturationImage).grow();
        if (buttonListener != null) saturationImage.addListener(buttonListener);
        saturationImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                s = value;
                PopColorPicker.this.updateRGB();
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        saturationArrowImage = new Image(style.colorSliderKnobHorizontal);
        saturationArrowImage.setTouchable(Touchable.disabled);
        table.addActor(saturationArrowImage);

        saturationTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        saturationTextField.setProgrammaticChangeEvents(false);
        sliderTable.add(saturationTextField).width(50);
        if (textFieldListener != null) saturationTextField.addListener(textFieldListener);
        applyFieldListener(saturationTextField);
        saturationTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                s = Integer.parseInt(saturationTextField.getText()) / 100f;
                s = MathUtils.clamp(s, 0, 1);
                updateRGB();
                updateColorDisplay();
            }
        });

        table = new Table();
        sliderTable.add(table);

        table.defaults().space(3);
        imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                s += (isShifting() ? SHIFT_AMOUNT : 1) / 100f;
                if (s > 1) s = 1;
                updateRGB();
                updateColorDisplay();
            }
        });

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                s -= (isShifting() ? SHIFT_AMOUNT : 1) / 100f;
                if (s < 0) s = 0;
                updateRGB();
                updateColorDisplay();
            }
        });

        sliderTable.row();
        brightnessRadio = new ImageButton(style.radioButtonStyle);
        brightnessRadio.setProgrammaticChangeEvents(false);
        sliderTable.add(brightnessRadio);
        radioGroup.add(brightnessRadio);
        if (buttonListener != null) brightnessRadio.addListener(buttonListener);
        brightnessRadio.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateColorDisplay();
            }
        });

        brightnessLabel = new Label("B", style.labelStyle);
        sliderTable.add(brightnessLabel);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        sliderTable.add(table).width(180).fillY();

        brightnessImage = new Image();
        brightnessImage.setScaling(Scaling.stretch);
        table.add(brightnessImage).grow();
        if (buttonListener != null) brightnessImage.addListener(buttonListener);
        brightnessImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                br = value;
                PopColorPicker.this.updateRGB();
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        brightnessArrowImage = new Image(style.colorSliderKnobHorizontal);
        brightnessArrowImage.setTouchable(Touchable.disabled);
        table.addActor(brightnessArrowImage);

        brightnessTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        brightnessTextField.setProgrammaticChangeEvents(false);
        sliderTable.add(brightnessTextField).width(50);
        if (textFieldListener != null) brightnessTextField.addListener(textFieldListener);
        applyFieldListener(brightnessTextField);
        brightnessTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                br = Integer.parseInt(brightnessTextField.getText()) / 100f;
                br = MathUtils.clamp(br, 0, 1);
                updateRGB();
                updateColorDisplay();
            }
        });

        table = new Table();
        sliderTable.add(table);

        table.defaults().space(3);
        imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                br += (isShifting() ? SHIFT_AMOUNT : 1) / 100f;
                if (br > 1) br = 1;
                updateRGB();
                updateColorDisplay();
            }
        });

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);
        imageButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                br -= (isShifting() ? SHIFT_AMOUNT : 1) / 100f;
                if (br < 0) br = 0;
                updateRGB();
                updateColorDisplay();
            }
        });

        sliderTable.row();
        if (showAlpha) sliderTable.add();

        label = new Label("A", style.labelStyle);
        if (showAlpha) sliderTable.add(label);

        table = new Table();
        table.setClip(true);
        table.setBackground(style.colorSliderBackground);
        if (showAlpha) sliderTable.add(table).width(180).fillY();

        Stack stack = new Stack();
        table.add(stack).grow();

        image = new Image(style.checkerBackground);
        stack.add(image);

        alphaImage = new Image();
        alphaImage.setScaling(Scaling.stretch);
        stack.add(alphaImage);
        if (buttonListener != null) alphaImage.addListener(buttonListener);
        alphaImage.addListener(new ColorDragListener(new ColorDraggable() {
            @Override
            public void dragged(float value) {
                a = value;
                PopColorPicker.this.updateColorDisplay();
            }
        }));

        alphaArrowImage = new Image(style.colorSliderKnobHorizontal);
        alphaArrowImage.setTouchable(Touchable.disabled);
        table.addActor(alphaArrowImage);

        alphaTextField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        alphaTextField.setProgrammaticChangeEvents(false);
        if (showAlpha) sliderTable.add(alphaTextField).width(50);
        if (textFieldListener != null) alphaTextField.addListener(textFieldListener);
        applyFieldListener(alphaTextField);
        alphaTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                a = Integer.parseInt(alphaTextField.getText()) / 255f;
                a = MathUtils.clamp(a, 0, 1);
                updateColorDisplay();
            }
        });

        table = new Table();
        if (showAlpha) sliderTable.add(table);

        table.defaults().space(3);
        imageButton = new ImageButton(style.increaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);

        table.row();
        imageButton = new ImageButton(style.decreaseButtonStyle);
        table.add(imageButton);
        if (buttonListener != null) imageButton.addListener(buttonListener);

        controlTable.row();
        table = new Table();
        controlTable.add(table).growX().space(5);

        table.defaults().space(5);

        stack = new Stack();
        table.add(stack);

        if (oldColor != null) {
            stack.setTouchable(Touchable.enabled);
            if (buttonListener != null) stack.addListener(buttonListener);

            image = new Image(style.previewSwatchBackground);
            image.setScaling(Scaling.none);
            stack.add(image);

            swatchNewImage = new Image(style.previewSwatchNew);
            swatchNewImage.setScaling(Scaling.none);
            stack.add(swatchNewImage);

            image = new Image(style.previewSwatchOld);
            image.setColor(oldColor);
            image.setScaling(Scaling.none);
            stack.add(image);
        } else {
            image = new Image(style.previewSwatchSingleBackground);
            image.setScaling(Scaling.none);
            stack.add(image);

            swatchNewImage = new Image(style.previewSwatchSingle);
            swatchNewImage.setScaling(Scaling.none);
            stack.add(swatchNewImage);
        }
        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (oldColor != null) {
                    System.out.println(x + " " + y);
                    r = oldColor.r;
                    g = oldColor.g;
                    b = oldColor.b;
                    a = oldColor.a;
                    updateHSB();
                    updateColorDisplay();
                }
            }
        });

        hexTextField = new TextField("", style.hexTextFieldStyle) {
            @Override
            public void next(boolean up) {
                nextTextField(up, this);
            }
        };
        hexTextField.setProgrammaticChangeEvents(false);
        hexTextField.setMaxLength(8);
        hexTextField.setTextFieldFilter(new TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField, char c) {
                if (Character.isDigit(c)) return true;
                else switch (c) {
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                        return true;
                    default:
                        return false;
                }
            }
        });
        table.add(hexTextField).width(80);
        if (textFieldListener != null) hexTextField.addListener(textFieldListener);
        applyFieldListener(hexTextField);
        hexTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text = hexTextField.getText();
                if (text.length() == 6) {
                    text += "FF";
                    Color color = Color.valueOf(text);
                    r = color.r;
                    g = color.g;
                    b = color.b;
                    a = 1;
                    updateHSB();
                    updateColorDisplay();
                } else if (text.length() == 8) {
                    Color color = Color.valueOf(text);
                    r = color.r;
                    g = color.g;
                    b = color.b;
                    a = color.a;
                    updateHSB();
                    updateColorDisplay();
                }
            }
        });

        TextButton textButton = new TextButton("OK", style.textButtonStyle);
        table.add(textButton).width(70);
        if (buttonListener != null) textButton.addListener(buttonListener);
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                eventColor.set(r, g, b, a);
                fire(new PopColorPickerEvent(eventColor, Status.picked));
            }
        });

        textButton = new TextButton("Cancel", style.textButtonStyle);
        table.add(textButton).width(70);
        if (buttonListener != null) textButton.addListener(buttonListener);
        textButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                fire(new PopColorPickerEvent(oldColor, Status.cancelled));
            }
        });
    }

    private void applyFieldListener(final TextField textField) {
        textField.removeListener(textField.getDefaultInputListener());
        textField.addListener(new ClickListener() {
            boolean selectAll;
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!selectAll) ((ClickListener)textField.getDefaultInputListener()).clicked(event, x, y);
                else {
                    textField.selectAll();
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                selectAll = getStage().getKeyboardFocus() != event.getListenerActor();
                textField.getDefaultInputListener().touchDown(event, x, y, pointer, button);
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                textField.getDefaultInputListener().touchDragged(event, x, y, pointer);
                super.touchDragged(event, x, y, pointer);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                textField.getDefaultInputListener().touchUp(event, x, y, pointer, button);
                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                return textField.getDefaultInputListener().keyDown(event, keycode);
            }

            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                return textField.getDefaultInputListener().keyUp(event, keycode);
            }

            @Override
            public boolean keyTyped(InputEvent event, char character) {
                return textField.getDefaultInputListener().keyTyped(event, character);
            }
        });
    }

    @Override
    public void show(Stage stage, Action action) {
        super.show(stage, action);
        updateColorDisplay();
        hexTextField.selectAll();
        stage.setKeyboardFocus(hexTextField);
    }

    private void updateRGB() {
        if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, s, br, a));
        else tempColor.set(ColorUtils.hsb2rgb(h, s, br, a));
        r = tempColor.r;
        g = tempColor.g;
        b = tempColor.b;
    }

    private void updateHSB() {
        if (hslTextButton.isChecked()) tempColor.set(ColorUtils.rgb2hsl(r, g, b, a));
        else tempColor.set(ColorUtils.rgb2hsb(r, g, b, a));
        if (!MathUtils.isZero(r) || !MathUtils.isZero(g) || !MathUtils.isZero(b))
            if (!MathUtils.isEqual(1,r) || !MathUtils.isEqual(1, g) || !MathUtils.isEqual(1, b))
                h = tempColor.r;
        if (!MathUtils.isZero(r) || !MathUtils.isZero(g) || !MathUtils.isZero(b))
            s = tempColor.g;
        br = tempColor.b;
        tempColor.set(ColorUtils.rgb2hsl(r, g, b, a));
    }

    private void updateColorDisplay() {
        eventColor.set(r, g, b, a);
        fire(new PopColorPickerEvent(eventColor, Status.updated));

        if (hslTextButton.isChecked()) brightnessLabel.setText("L");
        else brightnessLabel.setText("B");

        if (redRadio.isChecked()) {
            squareModelCircleStack.setX(b * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            squareModelCircleStack.setY(g * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            verticalModelArrowImage.setY(r * verticalModelImage.getHeight() - verticalModelArrowImage.getHeight() / 2);
        } else if (greenRadio.isChecked()) {
            squareModelCircleStack.setX(b * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            squareModelCircleStack.setY(r * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            verticalModelArrowImage.setY(g * verticalModelImage.getHeight() - verticalModelArrowImage.getHeight() / 2);
        } else if (blueRadio.isChecked()) {
            squareModelCircleStack.setX(r * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            squareModelCircleStack.setY(g * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            verticalModelArrowImage.setY(b * verticalModelImage.getHeight() - verticalModelArrowImage.getHeight() / 2);
        } else if (hueRadio.isChecked()) {
            squareModelCircleStack.setX(s * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            squareModelCircleStack.setY(br * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            verticalModelArrowImage.setY(h * verticalModelImage.getHeight() - verticalModelArrowImage.getHeight() / 2);
        } else if (saturationRadio.isChecked()) {
            squareModelCircleStack.setX(h * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            squareModelCircleStack.setY(br * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            verticalModelArrowImage.setY(s * verticalModelImage.getHeight() - verticalModelArrowImage.getHeight() / 2);
        } else if (brightnessRadio.isChecked()) {
            squareModelCircleStack.setX(h * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            squareModelCircleStack.setY(s * squareModelImage.getWidth() - squareModelCircleStack.getWidth()/ 2);
            verticalModelArrowImage.setY(br * verticalModelImage.getHeight() - verticalModelArrowImage.getHeight() / 2);
        }

        squareModelCircleImage.setColor(tempColor.set(r, g, b, 1f));

        TenPatchDrawable tenPatch = new TenPatchDrawable(whiteTenPatch);
        tenPatch.setColor1(tempColor.set(0, g, b, 1));
        tenPatch.setColor2(tempColor);
        tenPatch.setColor3(tempColor.set(1, g, b, 1));
        tenPatch.setColor4(tempColor);
        redImage.setDrawable(tenPatch);

        redArrowImage.setX(r * redImage.getWidth() - redArrowImage.getWidth() / 2);

        tenPatch = new TenPatchDrawable(whiteTenPatch);
        tenPatch.setColor1(tempColor.set(r, 0, b, 1));
        tenPatch.setColor2(tempColor);
        tenPatch.setColor3(tempColor.set(r, 1, b, 1));
        tenPatch.setColor4(tempColor);
        greenImage.setDrawable(tenPatch);

        greenArrowImage.setX(g * greenImage.getWidth() - greenArrowImage.getWidth() / 2);

        tenPatch = new TenPatchDrawable(whiteTenPatch);
        tenPatch.setColor1(tempColor.set(r, g, 0, 1));
        tenPatch.setColor2(tempColor);
        tenPatch.setColor3(tempColor.set(r, g, 1, 1));
        tenPatch.setColor4(tempColor);
        blueImage.setDrawable(tenPatch);

        blueArrowImage.setX(b * blueImage.getWidth() - blueArrowImage.getWidth() / 2);

        hueArrowImage.setX(h * hueImage.getWidth() - hueArrowImage.getWidth() / 2);

        tenPatch = new TenPatchDrawable(whiteTenPatch);
        if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, 0f, br, 1f));
        else tempColor.set(ColorUtils.hsb2rgb(h, 0f, br, 1f));
        tenPatch.setColor1(tempColor);
        tenPatch.setColor2(tempColor);
        if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, 1f, br, 1f));
        else tempColor.set(ColorUtils.hsb2rgb(h, 1f, br, 1f));
        tenPatch.setColor3(tempColor);
        tenPatch.setColor4(tempColor);
        saturationImage.setDrawable(tenPatch);

        saturationArrowImage.setX(s * saturationImage.getWidth() - saturationArrowImage.getWidth() / 2);

        TextureRegionDrawable drawable = ((TextureRegionDrawable) brightnessImage.getDrawable());
        if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
        brightnessImage.setDrawable(new TextureRegionDrawable(generateHorizontalBrightness(180)));

        brightnessArrowImage.setX(br * brightnessImage.getWidth() - brightnessArrowImage.getWidth() / 2);

        tenPatch = new TenPatchDrawable(whiteTenPatch);
        tenPatch.setColor1(tempColor.set(r, g, b, 0.0f));
        tenPatch.setColor2(tempColor);
        tenPatch.setColor3(tempColor.set(r, g, b, 1.0f));
        tenPatch.setColor4(tempColor);
        alphaImage.setDrawable(tenPatch);

        alphaArrowImage.setX(a * alphaImage.getWidth() - alphaArrowImage.getWidth() / 2);

        tempColor.set(r, g, b, a);
        swatchNewImage.setColor(tempColor);

        if (radioGroup.getChecked() == redRadio) {
            drawable = (TextureRegionDrawable) squareModelImage.getDrawable();
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            squareModelImage.setDrawable(new TextureRegionDrawable(generateColorVsGreenAndBlue(tempColor, 250, 250)));

            drawable = (TextureRegionDrawable) verticalModelImage.getDrawable();
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            verticalModelImage.setDrawable(generateVerticalRed(tempColor));
        } else if (radioGroup.getChecked() == greenRadio) {
            drawable = ((TextureRegionDrawable) squareModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            squareModelImage.setDrawable(new TextureRegionDrawable(generateColorVsRedAndBlue(tempColor, 250, 250)));

            drawable = ((TextureRegionDrawable) verticalModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            verticalModelImage.setDrawable(generateVerticalGreen(tempColor));
        } else if (radioGroup.getChecked() == blueRadio) {
            drawable = ((TextureRegionDrawable) squareModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            squareModelImage.setDrawable(new TextureRegionDrawable(generateColorVsGreenAndRed(tempColor, 250, 250)));

            drawable = ((TextureRegionDrawable) verticalModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            verticalModelImage.setDrawable(generateVerticalBlue(tempColor));
        } else if (radioGroup.getChecked() == hueRadio) {
            drawable = ((TextureRegionDrawable) squareModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            squareModelImage.setDrawable(new TextureRegionDrawable(generateColorVsBrightnessAndSaturation(tempColor, 250, 250)));

            drawable = ((TextureRegionDrawable) verticalModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            verticalModelImage.setDrawable(new TextureRegionDrawable(generateVerticalHue(250)));
        } else if (radioGroup.getChecked() == saturationRadio) {
            drawable = ((TextureRegionDrawable) squareModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            squareModelImage.setDrawable(new TextureRegionDrawable(generateHueVsBrightness(250, 250)));

            drawable = ((TextureRegionDrawable) verticalModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            verticalModelImage.setDrawable(new TextureRegionDrawable(generateVerticalSaturation(tempColor, 250)));
        } else if (radioGroup.getChecked() == brightnessRadio) {
            drawable = ((TextureRegionDrawable) squareModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            squareModelImage.setDrawable(new TextureRegionDrawable(generateHueVsSaturation(250, 250)));

            drawable = ((TextureRegionDrawable) verticalModelImage.getDrawable());
            if (drawable != null && !(drawable instanceof TenPatchDrawable)) drawable.getRegion().getTexture().dispose();
            verticalModelImage.setDrawable(new TextureRegionDrawable(generateVerticalBrightness(tempColor, 250)));
        }

        redTextField.setText(Integer.toString(MathUtils.round(r * 255)));
        greenTextField.setText(Integer.toString(MathUtils.round(g * 255)));
        blueTextField.setText(Integer.toString(MathUtils.round(b * 255)));

        hueTextField.setText(Integer.toString(MathUtils.round(h * 360)));
        saturationTextField.setText(Integer.toString(MathUtils.round(s * 100)));
        brightnessTextField.setText(Integer.toString(MathUtils.round(br * 100)));

        alphaTextField.setText(Integer.toString(MathUtils.round(a * 255)));

        tempColor.set(r, g, b, a);
        if (getStage().getKeyboardFocus() != hexTextField) hexTextField.setText(tempColor.toString().toUpperCase(Locale.ROOT));
    }

    private TenPatchDrawable createWhiteTenPatch() {
        if (whiteTenPatch == null) {
            Pixmap pixmap = new Pixmap(1, 1, Format.RGBA4444);
            pixmap.setColor(Color.WHITE);
            pixmap.drawPixel(0, 0);
            Texture texture = new Texture(pixmap);
            TextureRegion textureRegion = new TextureRegion(texture);
            whiteTenPatch = new TenPatchDrawable(new int[]{0, 0}, new int[]{0, 0}, false, textureRegion);
        }

        return whiteTenPatch;
    }

    private TextureRegion generateHorizontalHue(int width) {
        Pixmap pixmap = new Pixmap(width, 1, Format.RGBA8888);
        for (int i = 0; i < width; i++) {
            if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb((float) i / width, 1f, 1f, 1f));
            else tempColor.set(ColorUtils.hsb2rgb((float) i / width, 1f, 1f, 1f));
            pixmap.setColor(tempColor);
            pixmap.drawPixel(i, 0);
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateHorizontalBrightness(int width) {
        Pixmap pixmap = new Pixmap(width, 1, Format.RGBA8888);
        for (int i = 0; i < width; i++) {
            float newBr = (float) i / width;
            if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, s, newBr, 1f));
            else tempColor.set(ColorUtils.hsb2rgb(h, s, newBr, 1f));
            pixmap.setColor(tempColor);
            pixmap.drawPixel(i, 0);
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TenPatchDrawable generateVerticalRed(Color color) {
        TenPatchDrawable tenPatchDrawable = new TenPatchDrawable(whiteTenPatch);

        tempColor.set(0f, color.g, color.b, 1f);
        tenPatchDrawable.setColor1(tempColor);
        tenPatchDrawable.setColor4(tempColor);

        tempColor.set(1f, color.g, color.b, 1f);
        tenPatchDrawable.setColor2(tempColor);
        tenPatchDrawable.setColor3(tempColor);

        return tenPatchDrawable;
    }

    private TenPatchDrawable generateVerticalGreen(Color color) {
        TenPatchDrawable tenPatchDrawable = new TenPatchDrawable(whiteTenPatch);

        tempColor.set(color.r, 0f, color.b, 1f);
        tenPatchDrawable.setColor1(tempColor);
        tenPatchDrawable.setColor4(tempColor);

        tempColor.set(color.r, 1f, color.b, 1f);
        tenPatchDrawable.setColor2(tempColor);
        tenPatchDrawable.setColor3(tempColor);

        return tenPatchDrawable;
    }

    private TenPatchDrawable generateVerticalBlue(Color color) {
        TenPatchDrawable tenPatchDrawable = new TenPatchDrawable(whiteTenPatch);

        tempColor.set(color.r, color.g, 0f, 1f);
        tenPatchDrawable.setColor1(tempColor);
        tenPatchDrawable.setColor4(tempColor);

        tempColor.set(color.r, color.g, 1f, 1f);
        tenPatchDrawable.setColor2(tempColor);
        tenPatchDrawable.setColor3(tempColor);

        return tenPatchDrawable;
    }

    private TextureRegion generateVerticalHue(int height) {
        Pixmap pixmap = new Pixmap(1, height, Format.RGBA8888);
        for (int i = 0; i < height; i++) {
            if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb((float) (height - i) / height, 1f, .5f, 1f));
            else tempColor.set(ColorUtils.hsb2rgb((float) (height - i) / height, 1f, 1f, 1f));
            pixmap.setColor(tempColor);
            pixmap.drawPixel(0, i);
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateVerticalSaturation(Color color, int height) {
        Pixmap pixmap = new Pixmap(1, height, Format.RGBA8888);
        for (int i = 0; i < height; i++) {
            float newS = (float) (height - i) / height;
            if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, newS, br, 1f));
            else tempColor.set(ColorUtils.hsb2rgb(h, newS, br, 1f));
            pixmap.setColor(tempColor);
            pixmap.drawPixel(0, i);
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateVerticalBrightness(Color color, int height) {
        Pixmap pixmap = new Pixmap(1, height, Format.RGBA8888);
        for (int i = 0; i < height; i++) {
            float newBr = (float) (height - i) / height;
            if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, s, newBr, 1f));
            else tempColor.set(ColorUtils.hsb2rgb(h, s, newBr, 1f));
            pixmap.setColor(tempColor);
            pixmap.drawPixel(0, i);
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateHueVsSaturation(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb((float) x / width, (float) (height - y) / height, br, 1f));
                else tempColor.set(ColorUtils.hsb2rgb((float) x / width, (float) (height - y) / height, br, 1f));
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
            }
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateHueVsBrightness(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb((float) x / width, s, (float) (height - y) / height, 1f));
                else tempColor.set(ColorUtils.hsb2rgb((float) x / width, s, (float) (height - y) / height, 1f));
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
            }
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateColorVsBrightnessAndSaturation(Color color, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (hslTextButton.isChecked()) tempColor.set(ColorUtils.hsl2rgb(h, (float) x / width, (float) (height - y) / height, 1f));
                else tempColor.set(ColorUtils.hsb2rgb(h, (float) x / width, (float) (height - y) / height, 1f));
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
            }
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateColorVsGreenAndBlue(Color color, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempColor.set(color.r, (float) (height - y) / height, (float) x / width, 1f);
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
            }
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateColorVsRedAndBlue(Color color, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempColor.set((float) (height - y) / height, color.g, (float) x / width, 1f);
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
            }
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private TextureRegion generateColorVsGreenAndRed(Color color, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempColor.set((float) x / width, (float) (height - y) / height, color.b, 1f);
                pixmap.setColor(tempColor);
                pixmap.drawPixel(x, y);
            }
        }
        return new TextureRegion(new Texture(pixmap));
    }

    private void nextTextField(boolean up, TextField current) {
        Array<TextField> textFields = new Array<TextField>(new TextField[] {redTextField, greenTextField, blueTextField, hueTextField, saturationTextField, brightnessTextField, alphaTextField, hexTextField});
        int index = textFields.indexOf(current, true);
        if (up) {
            index--;
        } else {
            index++;
        }
        if (index < 0) index = textFields.size - 1;
        else if (index >= textFields.size) index = 0;

        TextField textField = textFields.get(index);
        getStage().setKeyboardFocus(textField);
        textField.selectAll();
    }

    private static boolean isShifting() {
        return Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
    }

    public boolean isShowAlpha() {
        return showAlpha;
    }

    public void setShowAlpha(boolean showAlpha) {
        this.showAlpha = showAlpha;
    }

    public EventListener getButtonListener() {
        return buttonListener;
    }

    public void setButtonListener(EventListener buttonListener) {
        this.buttonListener = buttonListener;
        populate();
    }

    public EventListener getTextFieldListener() {
        return textFieldListener;
    }

    public void setTextFieldListener(EventListener textFieldListener) {
        this.textFieldListener = textFieldListener;
        populate();
    }

    private static class ColorDragListener extends DragListener {
        public ColorDraggable draggable;
        public ColorDragListener(ColorDraggable draggable) {
            this.draggable = draggable;
            setTapSquareSize(0);
        }

        @Override
        public void drag(InputEvent event, float x, float y, int pointer) {
            super.drag(event, x, y, pointer);
            float value = x / event.getListenerActor().getWidth();
            value = MathUtils.clamp(value, 0, 1);
            draggable.dragged(value);
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            float value = x / event.getListenerActor().getWidth();
            value = MathUtils.clamp(value, 0, 1);
            draggable.dragged(value);
            return super.touchDown(event, x, y, pointer, button);
        }
    }

    private interface ColorDraggable {
        void dragged(float value);
    }

    private class VerticalModelDragListener extends DragListener {
        public VerticalModelDragListener() {
            setTapSquareSize(0);
        }

        @Override
        public void drag(InputEvent event, float x, float y, int pointer) {
            super.drag(event, x, y, pointer);
            float value = y / event.getListenerActor().getHeight();
            value = MathUtils.clamp(value, 0, 1);
            dragged(value);
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            float value = y / event.getListenerActor().getHeight();
            value = MathUtils.clamp(value, 0, 1);
            dragged(value);
            return super.touchDown(event, x, y, pointer, button);
        }

        private void dragged(float value) {
            if (redRadio.isChecked()) {
                r = value;
                updateHSB();
                updateColorDisplay();
            } else if (greenRadio.isChecked()) {
                g = value;
                updateHSB();
                updateColorDisplay();
            } else if (blueRadio.isChecked()) {
                b = value;
                updateHSB();
                updateColorDisplay();
            } else if (hueRadio.isChecked()) {
                h = value;
                updateRGB();
                updateColorDisplay();
            } else if (saturationRadio.isChecked()) {
                s = value;
                updateRGB();
                updateColorDisplay();
            } else if (brightnessRadio.isChecked()) {
                br = value;
                updateRGB();
                updateColorDisplay();
            }
        }
    }

    private class SquareModelDragListener extends DragListener {
        public SquareModelDragListener() {
            setTapSquareSize(0);
        }

        @Override
        public void drag(InputEvent event, float x, float y, int pointer) {
            super.drag(event, x, y, pointer);
            dragged(MathUtils.clamp(x / event.getListenerActor().getWidth(), 0, 1), MathUtils.clamp(y / event.getListenerActor().getHeight(), 0, 1));
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            dragged(MathUtils.clamp(x / event.getListenerActor().getWidth(), 0, 1), MathUtils.clamp(y / event.getListenerActor().getHeight(), 0, 1));
            return super.touchDown(event, x, y, pointer, button);
        }

        private void dragged(float xValue, float yValue) {
            if (redRadio.isChecked()) {
                g = yValue;
                b = xValue;
                updateHSB();
                updateColorDisplay();
            } else if (greenRadio.isChecked()) {
                r = yValue;
                b = xValue;
                updateHSB();
                updateColorDisplay();
            } else if (blueRadio.isChecked()) {
                r = xValue;
                g = yValue;
                updateHSB();
                updateColorDisplay();
            } else if (hueRadio.isChecked()) {
                s = xValue;
                br = yValue;
                updateRGB();
                updateColorDisplay();
            } else if (saturationRadio.isChecked()) {
                h = xValue;
                br = yValue;
                updateRGB();
                updateColorDisplay();
            } else if (brightnessRadio.isChecked()) {
                h = xValue;
                s = yValue;
                updateRGB();
                updateColorDisplay();
            }
        }
    }

    public static class PopColorPickerEvent extends Event {
        public Color color;
        public enum Status {picked, updated, cancelled}
        public Status status;

        public PopColorPickerEvent(Color color, Status status) {
            this.color = color;
            this.status = status;
        }
    }

    public static abstract class PopColorPickerListener implements EventListener {
        @Override
        public boolean handle(Event event) {
            if (event instanceof PopColorPickerEvent) {
                PopColorPickerEvent pickerEvent = (PopColorPickerEvent)event;
                if (pickerEvent.status == Status.cancelled) cancelled(pickerEvent.color);
                else if (pickerEvent.status == Status.updated) updated(pickerEvent.color);
                else picked(pickerEvent.color);
                return true;
            }
            return false;
        }

        public abstract void picked(Color color);

        public abstract void updated(Color color);

        public abstract void cancelled(Color oldColor);
    }

    public static class PopColorPickerAdapter extends PopColorPickerListener {
        @Override
        public void picked(Color color) {

        }

        public void updated(Color color) {

        }

        @Override
        public void cancelled(Color oldColor) {

        }
    }

    public void setStyle(PopColorPickerStyle style) {
        super.setStyle(style);
        setBackground((Drawable) null);
    }

    public static class PopColorPickerStyle extends PopTableStyle {
        public Drawable titleBarBackground;
        public LabelStyle labelStyle;
        public TextButtonStyle textButtonStyle;
        public TextButtonStyle fileTextButtonStyle;
        public ScrollPaneStyle scrollPaneStyle;
        public Drawable colorSwatch;
        public Drawable colorSwatchNew;
        public Drawable colorSwatchPopBackground;
        public Drawable previewSwatchBackground;
        public Drawable previewSwatchOld;
        public Drawable previewSwatchNew;
        public Drawable previewSwatchSingleBackground;
        public Drawable previewSwatchSingle;
        public TextFieldStyle textFieldStyle;
        public TextFieldStyle hexTextFieldStyle;
        public Drawable colorSwatchPopPreview;
        public Drawable colorSliderBackground;
        public Drawable colorSliderKnobHorizontal;
        public Drawable colorSliderKnobVertical;
        public Drawable colorKnobCircleBackground;
        public Drawable colorKnobCircleForeground;
        public ImageButtonStyle radioButtonStyle;
        public ImageButtonStyle increaseButtonStyle;
        public ImageButtonStyle decreaseButtonStyle;
        public Drawable checkerBackground;
    }
}
