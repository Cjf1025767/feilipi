/**
 * 录音查询(model)
 */
Ext.define('Tab.view.record.SearchRecordModel', {
    extend: 'Ext.data.Model',
    idProperty:'nofield',
    fields: [{
            name: 'sGuid',
            mapping: 'id'
        },
        {
            name: 'bLock',
            mapping: 'lock'
        },
        {
            name: 'bDel',
            mapping: 'delete'
        },
        {
            name: 'bBak',
            mapping: 'backup'
        },
        {
            name: 'sSystemTim',
            mapping: 'createdate',
            convert: function (v, r) {
                return Ext.Date.format(new Date(v), 'Y-m-d H:i:s');
            }
        },
        {
            name: 'nSeconds',
            mapping: 'seconds'
        },
        {
            name: 'nDirection',
            mapping: 'direction'
        },
        {
            name: 'sCaller',
            mapping: 'caller'
        },
        {
            name: 'sCalled',
            mapping: 'called'
        },
        {
            name: 'sExtension',
            mapping: 'extension'
        },
        {
            name: 'sAgent',
            mapping: 'agent'
        },
        {
            name: 'sUserName',
            mapping: 'username'
        },
        {
            name: 'nHost',
            mappping: 'host'
        },
        {
            name: 'nChannel',
            mapping: 'channel'
        },
        {
            name: 'sFilename',
            mapping: 'filename'
        },
        {
            name: 'sGroupGuid',
            mapping: 'role.roleguid'
        },
        // {
        //     name: 'sGroupName',
        //     mapping: 'role.rolename'
        // },
        {
            name: 'sUcid',
            mapping: 'ucid'
        }
    ]
});

Ext.define('Tab.view.record.SearchRecordStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.record.SearchRecordModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/FLPrec/UISearchRecords',
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

Ext.define('Tab.view.record.RecordSummaryModel', {
    extend: 'Ext.data.Model',
    //fields: []
});

Ext.define('Tab.view.record.RecordSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.record.RecordSummaryModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rec/UIReportRecSummary',
        timeout: 120000,
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'list'
        }
    }
});

Ext.define('Tab.view.record.groupsMenuModel', {
    extend: 'Ext.data.Model',
    idProperty: 'roleguid',
    fields: [{
            type: 'string',
            name: 'roleguid'
        },
        {
            type: 'string',
            name: 'rolename'
        }
    ]
});

Ext.define('Tab.view.record.groupsMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.record.groupsMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetRolesManageable',
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    }
});

Ext.define('Tab.view.record.typeModel', {
    extend: 'Ext.data.Model',
    fields: [{
            type: 'string',
            name: 'name'
        },
        {
            type: 'integer',
            name: 'id'
        }
    ]
});