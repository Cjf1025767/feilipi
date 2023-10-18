Ext.define('Tab.view.dashboard.DocumentModel', {
    extend: 'Ext.data.Model',
    fields: [{
            name: 'header',
            convert: function () {
                return '<span class="x-fa fa-file-text-o bashboard-header-mouse"></span>';
            }
        },
        {
            name: 'modifiedname',
            mapping: 'crmentity.modifiedname'
        },
        {
            name: 'modifiedtime',
            mapping: function (v) {
                return Ext.Date.format(new Date(v.crmentity.modifiedtime), 'Y-m-d H:i:s');
            }
        }
    ]
});

Ext.define('Tab.view.dashboard.DocumentStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.dashboard.DocumentModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        //url: 动态设置
        timeout: 120000,
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'docs',
            totalProperty: 'totalCount'
        }
    }
});

Ext.define('Tab.view.dashboard.DocumentTreeStore', {
    extend: 'Ext.data.TreeStore',
    autoLoad: false,
    parentIdProperty: 'fatherid',
    proxy: {
        type: 'ajax',
        url: '/tab/doc/GetDocsTreeNodes',
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'docs'
        }
    },
    root: {
        text: '目录',
        iconCls: 'x-fa fa-home',
        id: '00000000-0000-0000-0000-000000000000',
        expanded: true
    },
    fields: [
        {
            name:'id',
            mapping: function (v) {
                if (typeof (v) == 'undefined' || v==='00000000-0000-0000-0000-000000000000') return '00000000-0000-0000-0000-000000000000'; //Root
                return v.tid.tree;
            }
        },
        {
            name:'fatherid',
            mapping: function (v) {
                if (typeof (v) === 'undefined' || v==='00000000-0000-0000-0000-000000000000') return '00000000-0000-0000-0000-000000000000'; //Root
                var pos = v.parentTree.split('::');
                var tid = pos.length>1 ? pos[pos.length-2] : '';
                tid = tid==='' ? '00000000-0000-0000-0000-000000000000' : tid;
                return tid;
            }
        },
        {
            name: 'text',
            mapping: 'name'
        },
        {
            name: 'leaf',
            convert: function (v, r) {
                var childrens = r.get('childrens');
                if (typeof (childrens) == 'undefined') return false; //Root
                if (childrens != null && childrens.length > 0) {
                    return false;
                }
                return true;
            }
        },
        {
            name: 'readonly',
            type: 'bool'
        }
    ],
    folderSort: true,
    sorters: [{
        property: 'text',
        direction: 'ASC'
    }]
});