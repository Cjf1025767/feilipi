Ext.define('Tab.view.dashboard.DocumentsList', {
    extend: 'Ext.grid.Panel',
    xtype: 'documentslist',
    cls: 'email-inbox-panel shadow',
    requires: [
        'Tab.view.tab.iconlabel',
        'Tab.view.dashboard.DocumentsDetails',
        'Tab.view.tab.ResizeWindow'
    ],
    viewConfig: {
        preserveScrollOnRefresh: true,
        preserveScrollOnReload: true
    },
    title: 'Query',
    documentstreepanel: null,
    listeners: {
        celldblclick: function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
            var documentstreepanel = view.up().documentstreepanel;
            Ext.create({
                xtype: 'resizewindow',
                items: [
                    Ext.apply({
                        xtype: 'documentsdetails'
                    }, {
                        documentfatherid: documentstreepanel!=null ? documentstreepanel.store.proxy.extraParams.id : null,
                        documentid: record.get('knowledgebaseid'),
                        documentstreepanel: view.up().documentstreepanel,
                        documentslist: record,
                        documentcategory: documentstreepanel!=null ? documentstreepanel.store.proxy.extraParams.name : null
                    })
                ]
            });
        },
        cellclick: function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
            if (cellIndex == 0) {
                var documentstreepanel = view.up().documentstreepanel;
                Ext.create({
                    xtype: 'resizewindow',
                    items: [
                        Ext.apply({
                            xtype: 'documentsdetails'
                        }, {
                            documentfatherid: documentstreepanel!=null ? documentstreepanel.store.proxy.extraParams.id : null,
                            documentid: record.get('knowledgebaseid'),
                            documentstreepanel: view.up().documentstreepanel,
                            documentslist: record,
                            documentcategory: documentstreepanel!=null ? documentstreepanel.store.proxy.extraParams.name : null
                        })
                    ]
                });
            }
        }
    },

    headerBorders: false,
    rowLines: false,
    scrollable: false,
    store: Ext.create('Tab.view.dashboard.DocumentStore'),

    tbar: [{
            xtype: 'textfield',
            emptyText: 'Please input what you want to search',
            itemId: 'key',
            enableKeyEvents: true,
            flex: 1,
            listeners: {
                keydown: function (txt, e, opt) {
                    var me = txt;
                    if (e.getKey() == Ext.EventObject.ENTER) {
                        me.up().down('#keybtn').fireEvent('click', me.up().down('#keybtn'));
                    }
                }
            }
        },
        {
            ui: 'soft-green',
            text: 'Query',
            itemId: 'keybtn',
            listeners: {
                click: function (btn) {
                    var me = btn;
                    if (me.up().down('#key').getValue().length > 0) {
                        var store = me.up().up().store;
                        store.proxy.url = '/tab/doc/SearchDocuments';
                        Ext.apply(store.proxy.extraParams, {
                            key: me.up().down('#key').getValue()
                        });
                        store.reload();
                        me.up().up().getView().headerCt.child("#documentname").setText('标题');
                        me.up('documentslist').documentstreepanel = null;
                    }
                }
            }
        }
    ],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        dock: 'bottom',
        pageSize: 10,
        displayInfo: true,
        displayMsg: 'Display data from {0} to {1}, a total of {2}',
        emptyMsg: 'No Record',
        listeners: {
            beforerender: function (bgbar) {
                bgbar.add('-');
                bgbar.add(
                    new Ext.form.NumberField({
                        minWidth: 90,
                        maxWidth: 90,
                        minValue: 1,
                        maxValue: 1000,
                        step: 10,
                        value: bgbar.up().store.pageSize,
                        listeners: {
                            'change': function (field, newValue) {
                                bgbar.pageSize = newValue;
                                bgbar.up().store.pageSize = newValue;
                            }
                        }
                    }));
                bgbar.add('Records/Page');
                bgbar.setStore(bgbar.up().store);
            }
        }
    }],
    columns: [{
            dataIndex: 'header',
            menuDisabled: true,
            width: 34,
            sortable: false,
            tooltip: 'Add new document',
            text: '<span class="x-fa fa-plus bashboard-header-mouse"></span>',
            listeners: {
                headerclick: function (header, column, e, t, eOpts) {
                    var me = header;
                    var documentstreepanel = me.up('documentslist').documentstreepanel;
                    if (documentstreepanel != null) {
                        Ext.create({
                            xtype: 'resizewindow',
                            items: [
                                Ext.apply({
                                    xtype: 'documentsdetails'
                                }, {
                                    documentfatherid: documentstreepanel.store.proxy.extraParams.id,
                                    documentid: null,
                                    documentstreepanel: documentstreepanel,
                                    documentslist: me.up('documentslist'),
                                    documentcategory: documentstreepanel.store.proxy.extraParams.name
                                })
                            ]
                        });
                    }else{
                        Ext.Msg.alert('Error','A category needs to be selected');
                    }
                }
            }
        },
        {
            dataIndex: 'subject',
            itemId: 'documentname',
            menuDisabled: true,
            flex: 1,
            text: 'Title'
        },
        {
            dataIndex: 'number',
            menuDisabled: true,
            text: 'Number',
            width: 120
        },
        {
            dataIndex: 'modifiedname',
            menuDisabled: true,
            text: 'ModifiedName',
            width: 120
        },
        {
            menuDisabled: true,
            dataIndex: 'modifiedtime',
            width: 160,
            text: 'ModifiedTime'
        },
        {
            xtype: 'actioncolumn',
            align: 'center',
            text: 'Delete',
            tooltip: 'Delete',
            width: 50,
            menuDisabled: true,
            items: [{
                iconCls: 'x-fa fa-trash',
                tooltip: 'Delete',
                handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                    var rs = record;
                    var store = view.getStore();
                    var selModel = view.getSelectionModel();
                    Ext.MessageBox.confirm("Delete", "Please confirm the deletion",
                    function (btn, value) {
                        if (btn == 'yes') {
                            Ext.Ajax.request({
                                url : '/tab/doc/DeleteDocument',
                                method : 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                params: {
                                    id:rs.get('knowledgebaseid')
                                },
                                success: function(response, options){
                                    var obj = Ext.decode(response.responseText);
                                    if(obj.success){
                                        store.remove(rs);
                                        if (store.getCount() > 0) {
                                            selModel.select(0);
                                        }
                                    }else{
                                        Ext.Msg.alert("Error",obj.msg);
                                    }
                                },failure: function(response, opts) {
                                    console.log('server-side failure with status code ' + response.status);
                                }
                            });
                        }
                    });
                }
            }]
        }
    ]
});