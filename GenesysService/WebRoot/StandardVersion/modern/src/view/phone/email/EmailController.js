Ext.define('Tab.view.phone.email.EmailController', {
    extend: 'Tab.view.email.EmailController',
    alias: 'controller.email-phone',

    closeComposer: function () {
        var me = this,
            composer = me.composer,
            view = me.getView(),
            viewModel = me.getViewModel();

        if (composer) {
            view.remove(composer);
            me.composer = null;

            viewModel.set('composing', false);
        }
    },

    onPlusButtonTap: function () {
        var actions = this.actions;

        if (!actions || !actions.getDisplayed()) {
            this.doCompose();
        }
    },

    doCompose: function (to) {
        var me = this,
            composer = me.composer,
            view = me.getView(),
            viewModel = me.getViewModel(),
            toField;

        me.hideActions();

        if (!composer) {
            me.composer = composer = view.add({
                xtype: 'compose',
                flex: 1
            });

            if (to) {
                toField = me.lookup('toField');
                toField.setValue(to);
            }

            viewModel.set('composing', true);
        }
    },

    onChangeFilter: function (sender) {
        this.hideActions();
        this.callParent(arguments);
    },

    onCloseMessage: function () {
        this.closeComposer();
    },

    onLongPressCompose: function (e) {
        this.showActions();
    },

    onSendMessage: function () {
        this.closeComposer();
    },

    onSwipe: function (event) {
        if (event.direction === 'left') {
            this.showActions();
        }
    }
});
