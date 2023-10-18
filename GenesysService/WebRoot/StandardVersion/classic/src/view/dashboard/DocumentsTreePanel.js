Ext.define('Tab.view.dashboard.DocumentsTreePanel', {
    extend: 'Ext.tree.Panel',
    xtype: 'documentstreepanel',
    requires: [
        'Tab.view.dashboard.DocumentsDetails',
        'Tab.view.dashboard.DocumentTreeStore',
        'Tab.view.tab.ResizeWindow'
    ],
    scrollable: true,
    rootVisible: true,
    useArrows: true,
    listeners: {
        afterrender: function (me) {
            me.setStore(Ext.create('Tab.view.dashboard.DocumentTreeStore'));
        },
        itemclick: function (me, record, item, index, e, eOpts) {
            var tree = me.up();
            var fatherid = tree.store.proxy.extraParams.id;
            if (typeof (fatherid) != 'undefined' && fatherid.length > 0 && fatherid === record.get('id')) { } else {
                Ext.apply(tree.store.proxy.extraParams, {
                    id: record.get('id'),
                    name: record.get('name')
                });
            }
        },
        beforedrop: function (node, data, overModel, dropPosition, dropHandlers) {
            dropHandlers.wait = true;
            if (data.records.length > 0) {
                var tree = this;
                var reload = overModel.data.leaf;
                if (!reload) {
                    var node = tree.store.getNodeById(data.records[0].get('fatherid'));
                    if (node.childNodes.length == 1) reload = true;
                }
                Ext.Ajax.request({
                    url: '/tab/doc/ChangeFather',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    async: false,
                    params: {
                        id: data.records[0].get('id'),
                        fatherid: overModel.data.id
                    },
                    failure: function (response, opts) {
                        console.log('server-side failure with status code ' + response.status);
                    },
                    success: function (response, opts) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            dropHandlers.processDrop();
                            if (reload) {
                                tree.store.load();
                            }
                        } else {
                            dropHandlers.cancelDrop();
                        }
                    }

                });
            } else {
                dropHandlers.cancelDrop();
            }
        }
    },
    viewConfig: {
        plugins: {
            ptype: 'treeviewdragdrop',
            containerScroll: true
        }
    },
    columns: [{
        xtype: 'treecolumn', //this is so we know which column will show the tree
        text: 'knowledge base',
        dataIndex: 'text',
        menuDisabled: true,
        flex: 1,
        editor: {
            xtype: 'textfield',
            allowBlank: false
        },
        renderer: function (v, m, r) {
            var name = r.get('name');
            if (typeof (name) == 'undefined') return 'Catalogue'; //Root
            return name === 'LBL_NONE' ? 'No Category' : name;
        }
    },
    {
        xtype: 'actioncolumn',
        text: '<span class="x-fa fa-refresh bashboard-header-mouse"></span>',
        tooltip: 'Add/Del(Refresh)',
        width: 45,
        minWidth: 45,
        maxWidth: 45,
        menuDisabled: true,
        listeners: {
            headerclick: function (header, column, e, t, eOpts) {
                header.up("documentstreepanel").getStore().reload();
            }
        },
        items: [{
            iconCls: 'x-fa fa-plus bashboard-action-col',
            tooltip: 'New Catgory',
            handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                var tree = view;
                var category = record.get('id');
                var parentTree = record.get('parentTree');
                var depth = record.get('depth');
                Ext.MessageBox.prompt('New Catgory', 'Catgory Name:', function (btn, text) {
                    if (btn === 'ok') {
                        Ext.Ajax.request({
                            url: "/tab/doc/AddCategory",
                            method: 'POST',
                            params: {
                                category: category,
                                parentTree,parentTree,
                                depth:depth,
                                subject: text
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    tree.store.reload();
                                } else {
                                    Ext.Msg.alert("Error", obj.msg);
                                }
                            },
                            failure: function (response, options) {
                                Ext.Msg.alert('Error', 'server-side failure with status code ' + response.status)
                            }
                        });
                    }
                }, this);
            },
            getClass: function (value, metadata, record) {
                return record.get('readonly') ? 'x-hide-display' : 'x-fa fa-plus bashboard-action-col';
            }
        }, {
            iconCls: 'x-fa fa-times bashboard-action-col',
            tooltip: 'Delete Catgory',
            handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                var tree = view;
                var fatherid = tree.store.proxy.extraParams.id;
                Ext.Msg.confirm('Delete', 'This deletion is unrecoverable. Are you sure you want to delete it?', function (btn) {
                    if (btn == 'yes') {
                        if (typeof (fatherid) != 'undefined' && fatherid.length > 0 && fatherid === record.get('id')) { } else {
                            Ext.apply(tree.store.proxy.extraParams, {
                                id: record.get('id')
                            });
                        }
                        Ext.Ajax.request({
                            url: "/tab/doc/DeleteCategory",
                            method: 'POST',
                            params: {
                                id: record.get('id')
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    tree.store.reload();
                                } else {
                                    Ext.Msg.alert("Error", obj.msg);
                                }
                            },
                            failure: function (response, options) {
                                Ext.Msg.alert('Error', 'server-side failure with status code ' + response.status)
                            }
                        });
                    }
                });
            },
            getClass: function (value, metadata, record) {
                return record.get('readonly') ? 'x-hide-display' : 'x-fa fa-times bashboard-action-col';
            }
        }]
    }
    ],
    plugins: [
        Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 2,
            listeners: {
                edit: function (tree, e) {
                    if (e.field == 'text') {
                        var node = e;
                        var originalValue = node.originalValue;
                        var newValue = node.record.get('text');
                        var id = node.record.get('id');
                        Ext.Ajax.request({
                            url: '/tab/doc/UpdateCategory',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                id: id,
                                name: newValue
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            },
                            success: function (response, opts) {
                                var obj = Ext.decode(response.responseText);
                                if (!obj.success) {
                                    node.record.set('name', originalValue)
                                } else {
                                    node.record.set('name', newValue)
                                    node.record.commit();
                                }
                            }
                        });
                    }
                }
            }
        })
    ]
});