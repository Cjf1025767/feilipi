/**
 * 录音查询(model)
 */
 Ext.define('Tab.view.voicemail.voicemailModel', {
    extend: 'Ext.data.Model',
    fields: [
        {
            name: 'sGuid',
            mapping: 'id'
        },
        {
            name: 'starttime',
            mapping: 'starttime',
            // convert: function (v, r) {
            //     return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
            // }
        },
        {
            name: 'endtime',
            mapping: 'endtime',
            // convert: function (v, r) {
            //     return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
            // }
        },
        {
            name: 'phone',
            mapping: 'phone'
        },
        {
            name: 'length',
            mapping: 'length'
        },
    ]
});

Ext.define('Tab.view.voicemail.voicemailStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.voicemail.voicemailModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/FLPrec/UISearchVoicemail',
        timeout: 120000,
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'recordItems',
            totalProperty: 'totalCount'
        }
    }
});



