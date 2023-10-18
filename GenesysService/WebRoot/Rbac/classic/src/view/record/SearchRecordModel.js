/**
 * 录音查询(model)
 */
Ext.define('Rbac.view.record.SearchRecordModel', {
    extend: 'Ext.data.Model',
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
        {
            name: 'sGroupName',
            mapping: 'role.rolename'
        },
        {
            name: 'sMark',
            mapping: 'mark'
        },
        {
            name: 'sUcid',
            mapping: 'ucid'
        }
    ]
});

Ext.define('Rbac.view.record.SearchRecordStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.view.record.SearchRecordModel',
    autoLoad: false,
    remoteSort: true,
    proxy: {
        type: 'rest',
        url: '/tab/rec/UISearchRecords',
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

Ext.define('Rbac.view.record.RecordSummaryModel', {
    extend: 'Ext.data.Model',
    //fields: []
});

Ext.define('Rbac.view.record.RecordSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.view.record.RecordSummaryModel',
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

Ext.define('Rbac.view.record.groupsMenuModel', {
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

Ext.define('Rbac.view.record.groupsMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.view.record.groupsMenuModel',
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

Ext.define('Rbac.view.record.usersMenuModel', {
    extend: 'Ext.data.Model',
    idProperty: 'userguid',
    fields: [{
            type: 'string',
            name: 'userguid'
        },
        {
            type: 'string',
            name: 'nickname'
        }
    ]
});

Ext.define('Rbac.view.record.usersMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.view.record.usersMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetUsersForRoleManageable',
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'Users',
            totalProperty: 'totalCount'
        }
    }
});

Ext.define('Rbac.view.record.typeModel', {
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