Ext.define('Tab.view.profile.ShareUpdate', {
    extend: 'Ext.Panel',
    xtype: 'profileshare',

    requires: [
        'Ext.field.Text',
        'Ext.Toolbar'
    ],

    cls: 'share-panel',
    layout: 'fit',
    padding: 10,

    bbar: [{
        iconCls: 'x-fa fa-video-camera',
        ui: 'header'
    }, {
        iconCls: 'x-fa fa-camera',
        ui: 'header'
    }, {
        iconCls: 'x-fa fa-file',
        ui: 'header'
    }, '->', {
        text: 'Share',
        ui: 'soft-blue'
    }],

    items: [{
        xtype: 'textareafield',
        placeholder: 'What\'s on your mind?'
    }]
});
