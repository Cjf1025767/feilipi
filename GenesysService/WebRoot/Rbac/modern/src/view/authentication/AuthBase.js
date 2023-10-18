Ext.define('Rbac.view.authentication.AuthBase', {
    extend: 'Ext.Panel',

    requires: [
        'Ext.layout.VBox'
    ],

    baseCls: 'auth-locked',

    layout: {
        type: 'vbox',
        align: 'center',
        pack: 'center'
    }
});
