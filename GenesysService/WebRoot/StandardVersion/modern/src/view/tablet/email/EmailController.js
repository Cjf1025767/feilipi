Ext.define('Tab.view.tablet.email.EmailController', {
    extend: 'Tab.view.email.EmailController',
    alias: 'controller.email-tablet',

    closeComposer: function () {
        var composer = this.composer;

        if (composer) {
            this.composer = null;
            composer.destroy();
        }
    },

    doCompose: function (to) {
        var me = this,
            composer = me.composer,
            view = me.getView(),
            toField;

        me.hideActions();

        if (!composer) {
            me.composer = composer = Ext.Viewport.add({
                xtype: 'compose',
                floated: true,
                modal: true,
                centered: true,
                ownerCt: view,
                width: '80%',
                height: '80%'
            });

            if (to) {
                toField = me.lookupReference('toField');
                toField.setValue(to);
            }
        }

        composer.show();
    },

    onChangeFilter: function(sender) {
        this.hideActions();
        this.callParent([sender]);
    },

    onSwipe: function (event) {
        if (this.lookup('controls').isHidden() && event.direction === 'left') {
            this.showActions();
        }
    },

    onCloseMessage: function () {
        this.closeComposer();
    },

    onSendMessage: function () {
        this.closeComposer();
    }
});
