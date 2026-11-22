LDLib2UI.player('ldlib2_kjs_test', event => {

    let root = new UIElement()
    root.layout(l => l.width(180).height(90).paddingAll(6))
    root.style(s => s.background('rect(#202428, 4)'))

    let label = new Label()
    label.setId('kjs_label')
    label.setText('KJS label ready')
    root.addChild(label)

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
