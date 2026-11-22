let size = GuiSize.of(180, 90)
let pos = GuiPos.of(3, 4)
let vec = new Vector3f(1, 2, 3)
let tooltips = HoverTooltips.empty()

LDLib2UI.player('ldlib2_kjs_test', event => {

    let root = new UIElement()
    root.layout(l => l.width(size.width).height(size.height).paddingAll(6))
    root.style(s => s.background('rect(#202428, 4)'))
    root.setId("kjs_root")

    let label = new Label()
    label.setId('kjs_label')
    label.setText('KJS label ready')
    root.addChild(label)

    let swatch = new UIElement()
    swatch.setId('kjs_swatch')
    swatch.layout(l => l.width(40).height(10))
    swatch.style(s => s.background('rect(#ff3030, 2)'))
    root.addChild(swatch)

    let button = new Button()
    button.setId('kjs_button')
    button.setText('Press me')
    button.layout(l => l.width(80).height(20))
    let clicks = 0
    button.addEventListener(UIEvents.CLICK, e => {
        clicks++
        label.setText('KJS clicked ' + clicks)
    })
    root.addChild(button)

    event.modularUI = ModularUI.of(UI.of(root), event.player)
})
