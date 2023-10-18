
Ext.define('Tab.view.main.HolidayView', {
    extend: 'Ext.panel.Panel',
    xtype: 'holidayView',

    requires: [
        'Tab.view.main.HolidayViewController',
        'Tab.view.main.HolidayViewModel',
        'Ext.grid.Panel',
        'Tab.view.main.HolidayWindow'
    ],

    controller: 'main-holidayview',
    viewModel: {
        type: 'main-holidayview'
    },
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'gridpanel',
        width: '100%',
        border: 1,
        tbar: {
            autoScroll: true,
            items: [{
                xtype: 'iconlabel',
                iconCls: 'x-fa fa-connectdevelop',
                bind:{text: ' {workdayView}'}
            },
            {
                xtype: 'checkboxgroup',
                bind:{fieldLabel: '{Workdays}'},
                width: 650,
                margin:'0 0 0 20',
                lastDept: "",
                listeners: {
                    'beforerender': function (me) {
                        var weekboxs = me;
                        Ext.Ajax.request({
                            url: '/tab/call/ListHolidaysWeek',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                dept: me.lastDept
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    weekboxs.setValue(obj.weeks);
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    },
                    'change': function (me, newValue, oldValue) {
                        Ext.Ajax.request({
                            url: '/tab/call/UpdateHolidaysWeek',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                dept: me.lastDept,
                                weeks: JSON.stringify(newValue)
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (!obj.success) {
                                    Ext.Msg.alert("Update workday error!");
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    }
                },
                items: [
                    { padding:'0 10 0 0',bind:{boxLabel: '{Monday}'}, name: 'week1' },
                    { padding:'0 10 0 0',bind:{boxLabel: '{Tuesday}'}, name: 'week2' },
                    { padding:'0 10 0 0',bind:{boxLabel: '{Wednesday}'}, name: 'week3' },
                    { padding:'0 10 0 0',bind:{boxLabel: '{Thursday}'}, name: 'week4' },
                    { padding:'0 10 0 0',bind:{boxLabel: '{Friday}'}, name: 'week5' },
                    { padding:'0 10 0 0',bind:{boxLabel: '{Saturday}'}, name: 'week6' },
                    { padding:'0 10 0 0',bind:{boxLabel: '{Sunday}'}, name: 'week7' }
                ]
            },
                '->',
            {
                xtype: 'button',
                bind:{text: '{Worktime}'},
                width: 100,
                handler: function () {
                    var weekboxs = this.up('gridpanel').down('checkboxgroup');
                    Ext.create("Ext.window.Window", {
                        bind:{title: "{DutyTelephone}"},
                        width: 700,
                        height: 380,
                        modal: true,
                        resizable: false,
                        bodyPadding: '5 0 0 5',
                        //和父窗口共享controller
                        controller: 'main-holidayview',
                        //和父窗口共享viewModel
                        viewModel: {
                            type: 'main-holidayview'
                        },
                        items: [
                            {
                                xtype: "form",
                                defaults: {
                                    anchor: '100%'
                                },
                                fieldDefaults: {
                                    labelAlign: "left",
                                    flex: 1
                                },
                                items: [
                                    {
                                        xtype: "gridpanel",
                                        store: Ext.create('Tab.view.main.workphoneStore'),
                                        plugins: [{
                                            ptype: 'cellediting',
                                            clicksToEdit: 2,
                                        }],
                                        listeners: {
                                            'beforerender': function (me) {
                                                me.lastDept = weekboxs.lastDept;
                                                me.store.load({
                                                    params: {
                                                        dept: weekboxs.lastDept,
                                                        onlytime: true
                                                    }
                                                });
                                                var model = me.lookupViewModel();
                                                me.down('#DutyTelephoneTooltip').tooltip = model.get('DutyTelephoneTooltip');
                                            },
                                            'edit': function (editor, context, eOpts) {
                                                var ctx = context;
                                                Ext.Ajax.request({
                                                    url: '/tab/call/AddOrUpdateHolidays',
                                                    method: 'POST',
                                                    headers: {
                                                        'Content-Type': 'application/x-www-form-urlencoded'
                                                    },
                                                    params: {
                                                        callholidayid: ctx.record.get('callholidayid'),
                                                        phone: ctx.record.get('phone'),
                                                        starttime: ctx.record.get('starttime'),
                                                        endtime: ctx.record.get('endtime')
                                                    },
                                                    success: function (response, options) {
                                                        var obj = Ext.decode(response.responseText);
                                                        if (obj.success) {
                                                            ctx.record.commit();
                                                        } else {
                                                            ctx.record.reject();
                                                        }
                                                    },
                                                    failure: function (response, opts) {
                                                        ctx.record.reject();
                                                        console.log('server-side failure with status code ' + response.status);
                                                    }
                                                });
                                            }
                                        },
                                        columns: [
                                            {
                                                xtype: 'checkcolumn',
                                                sortable: false,
                                                disabled: true,
                                                width: 70,
                                                bind:{text: '{Work}'},
                                                dataIndex: 'activate'
                                            },
                                            {
                                                bind:{text: '{Week}'},
                                                dataIndex: 'id',
                                                sortable: false,
                                                readOnly: true,
                                                width: 100,
                                                renderer: function (v) {
                                                    switch (v) {
                                                        case 1: return 'Monday'
                                                        case 2: return 'Tuesday'
                                                        case 3: return 'Wednesday'
                                                        case 4: return 'Thursday'
                                                        case 5: return 'Friday'
                                                        case 6: return 'Saturday'
                                                        case 7: return 'Sunday'
                                                    }
                                                }
                                            }, {
                                                bind:{text: '{Department}'},
                                                dataIndex: 'dept',
                                                sortable: false,
                                                readOnly: true,
                                                width:112,
                                                flex: 1
                                            }, {
                                                itemId:'DutyTelephoneTooltip',
                                                bind:{text: '{DutyTelephone}'},
                                                dataIndex: 'phone',
                                                sortable: false,
                                                tooltip: '{DutyTelephoneTooltip}',
                                                width:130,
                                                flex: 1,
                                                editor: {
                                                    allowBlank: true
                                                }
                                            },
                                            {
                                                bind:{text: '{Starttime}'},
                                                dataIndex: 'starttime',
                                                sortable: false,
                                                width: 95,
                                                editor: {
                                                    completeOnEnter: false,
                                                    field: {
                                                        xtype: 'timefield',
                                                        editable: true,
                                                        submitFormat: 'H:i:s',
                                                        format: 'H:i:s',
                                                        allowBlank: true,
                                                        listeners: {
                                                            'focus': function (txt) {
                                                                var v = txt.getRawValue();
                                                                if (v.indexOf(":") < 0) {
                                                                    v = parseInt(v);
                                                                    if (isNaN(v)) {
                                                                        v = 0;
                                                                    }
                                                                    txt.setValue(Ext.Date.format(new Date(v), 'H:i:s'));
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                renderer: function (v) {
                                                    if (v == null) return "";
                                                    return Ext.Date.format(new Date(v), 'H:i:s')
                                                }
                                            },
                                            {
                                                bind:{text: '{Endtime}'},
                                                dataIndex: 'endtime',
                                                sortable: false,
                                                width: 95,
                                                editor: {
                                                    completeOnEnter: false,
                                                    field: {
                                                        xtype: 'timefield',
                                                        editable: true,
                                                        submitFormat: 'H:i:s',
                                                        format: 'H:i:s',
                                                        allowBlank: true,
                                                        listeners: {
                                                            'focus': function (txt) {
                                                                var v = txt.getRawValue();
                                                                if (v.indexOf(":") < 0) {
                                                                    v = parseInt(v);
                                                                    if (isNaN(v)) {
                                                                        v = 0;
                                                                    }
                                                                    txt.setValue(Ext.Date.format(new Date(v), 'H:i:s'));
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                renderer: function (v) {
                                                    if (v == null) return "";
                                                    return Ext.Date.format(new Date(v), 'H:i:s')
                                                }
                                            },
                                            {
                                                xtype: 'actioncolumn',
                                                align: 'center',
                                                bind:{text: '{Subsection}'},
                                                width: 90,
                                                menuDisabled: true,
                                                items: [{
                                                    iconCls: 'x-fa fa-calendar',
                                                    tooltip: 'Subsection',
                                                    handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                                                        Ext.create('Tab.view.main.HolidayWindow', {
                                                            week: record.get('id'),
                                                            lastDept: weekboxs.lastDept
                                                        }).show();
                                                    }
                                                }]
                                            }
                                        ]
                                    }]
                            }
                        ],
                        buttonAlign: 'center',
                        buttons: [
                            {
                                xtype: "button",
                                bind:{text: "{Refresh}"},
                                handler: function (me) {
                                    var btn = me;
                                    var grid = btn.up('window').down('gridpanel');
                                    grid.store.load({
                                        params: {
                                            dept: grid.lastDept,
                                            onlytime: true
                                        }
                                    });;
                                }
                            }, {
                                xtype: "button",
                                bind:{text: "{Close}"},
                                handler: function (me) {
                                    var btn = me;
                                    btn.up('window').close();
                                }
                            }
                        ]
                    }).show();
                }
            },
            {
                xtype: 'button',
                bind:{text: '{AddNew}'},
                width: 100,
                handler: function (btn) {
                    var grid = btn.up('gridpanel');
                    Ext.Ajax.request({
                        url: '/tab/call/AddOrUpdateHolidays',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            dept: grid.lastDept,
                            phone:'holiday'
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                grid.store.insert(0, obj.item);
                                grid.findPlugin('cellediting').startEditByPosition({
                                    row: 0,
                                    column: 1
                                });
                            }
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                }
            },
            {
                xtype: 'button',
                bind:{text: '{Refresh}'},
                width: 100,
                handler: function () {
                    var view = this.up().up();
                    var weekboxs = view.down('checkboxgroup');
                    view.store.load();
                    weekboxs.fireEvent('beforerender', weekboxs);
                }
            }
            ]
        },
        plugins: [{
            ptype: 'cellediting',
            clicksToEdit: 2,
        }],
        listeners: {
            'beforerender': function (me) {
                var me = this;
                me.store.load();
                var model = me.lookupViewModel();
                me.down('#DutyTelephoneMainTooltip').tooltip = model.get('DutyTelephoneMainTooltip');
            },
            'cellclick': function (view, td, cellIndex, record, tr, rowIndex, e, eOpts) {
                var me = view;
                var col = me.getHeaderCt().getHeaderAtIndex(td.cellIndex).dataIndex;
                var rs = record;
                if (col == 'activate') {
                    Ext.Ajax.request({
                        url: '/tab/call/AddOrUpdateHolidays',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            callholidayid: record.get('callholidayid'),
                            onlyactivate: true,
                            activate: record.get('activate')
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                rs.commit();
                            } else {
                                rs.reject();
                            }
                        },
                        failure: function (response, opts) {
                            rs.reject();
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                } else {
                    var weekboxs = me.up('gridpanel').down('checkboxgroup');
                    weekboxs.lastDept = rs.get('dept');
                    Ext.Ajax.request({
                        url: '/tab/call/ListHolidaysWeek',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            dept: rs.get('dept')
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                weekboxs.setValue(obj.weeks);
                            }
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                }
            },
            'edit': function (editor, context, eOpts) {
                var ctx = context;
                Ext.Ajax.request({
                    url: '/tab/call/AddOrUpdateHolidays',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    params: {
                        callholidayid: ctx.record.get('callholidayid'),
                        name: ctx.record.get('name'),
                        startdate: ctx.record.get('startdate'),
                        starttime: ctx.record.get('starttime'),
                        endtime: ctx.record.get('endtime'),
                        activate: ctx.record.get('activate'),
                        dept: ctx.record.get('dept'),
                        phone: ctx.record.get('phone')
                    },
                    success: function (response, options) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            ctx.record.commit();
                        } else {
                            ctx.record.reject();
                        }
                    },
                    failure: function (response, opts) {
                        ctx.record.reject();
                        console.log('server-side failure with status code ' + response.status);
                    }
                });
            }
        },
        store: Ext.create('Tab.view.main.holidayStore'),
        columns: [
            {
                bind:{text: '{holidayName}'},
                dataIndex: 'name',
                width: 258,
                flex: 1,
                editor: {
                    allowBlank: true
                }
            },
            {
                bind:{text: '{DepartmentID}'},
                dataIndex: 'dept',
                width: 110,
                // readOnly: true,
                editor: {
                    allowBlank: true
                }
                // editor:new Ext.form.ComboBox({ store: Ext.create('Ext.data.Store',{
                //     fields : ["value" ],
                //     data : [['857181'],['857204']]
                // }), 
                //     valueField :"value",displayField: "value",
                //     forceSelection: true, editable: false,
                //     triggerAction: 'all'})
            },
            {
                itemId:'DutyTelephoneMainTooltip',
                bind:{text: '{DutyTelephone}'},
                dataIndex: 'phone',
                width: 120,
                readOnly: true,
                tooltip: '{DutyTelephoneMainTooltip}',
                editor: {
                    allowBlank: true
                }
                // editor:new Ext.form.ComboBox({ store: Ext.create('Ext.data.Store',{
   
                //     fields : ["value" ],
                //     data : [['holiday'],['weekend'],['work']]
                // }), 
            
                //     valueField :"value",displayField: "value",
                //     forceSelection: true, editable: false,
                //     triggerAction: 'all'})
            },
            {
                xtype: 'checkcolumn',
                width: 105,
                bind:{text: '{Work}'},
                dataIndex: 'activate'
            },
            {
                bind:{text: '{DateColumnName}'},
                dataIndex: 'startdate',
                width: 90,
                editor: {
                    completeOnEnter: false,
                    field: {
                        xtype: 'datefield',
                        editable: true,
                        submitFormat: 'Y-m-d',
                        format: 'Y-m-d',
                        allowBlank: false
                    }
                },
                renderer: function (v) {
                    return Ext.Date.format(new Date(v), 'Y-m-d')
                }
            },
            {
                bind:{text: '{Starttime}'},
                dataIndex: 'starttime',
                width: 95,
                editor: {
                    completeOnEnter: false,
                    field: {
                        xtype: 'timefield',
                        editable: true,
                        submitFormat: 'H:i:s',
                        format: 'H:i:s',
                        allowBlank: true,
                        listeners: {
                            'focus': function (txt) {
                                var v = txt.getRawValue();
                                if (v.indexOf(":") < 0) {
                                    v = parseInt(v);
                                    if (isNaN(v)) {
                                        v = 0;
                                    }
                                    txt.setValue(Ext.Date.format(new Date(v), 'H:i:s'));
                                }
                            }
                        }
                    }
                },
                renderer: function (v) {
                    if (v == null) return "";
                    return Ext.Date.format(new Date(v), 'H:i:s')
                }
            },
            {
                bind:{text: '{Endtime}'},
                dataIndex: 'endtime',
                width: 95,
                editor: {
                    completeOnEnter: false,
                    field: {
                        xtype: 'timefield',
                        editable: true,
                        submitFormat: 'H:i:s',
                        format: 'H:i:s',
                        allowBlank: true,
                        listeners: {
                            'focus': function (txt) {
                                var v = txt.getRawValue();
                                if (v.indexOf(":") < 0) {
                                    v = parseInt(v);
                                    if (isNaN(v)) {
                                        v = 0;
                                    }
                                    txt.setValue(Ext.Date.format(new Date(v), 'H:i:s'));
                                }
                            }
                        }
                    }
                },
                renderer: function (v) {
                    if (v == null) return "";
                    return Ext.Date.format(new Date(v), 'H:i:s')
                }
            },
            {
                bind:{text: '{Createdate}'},
                dataIndex: 'createdate',
                width: 155,
                renderer: function (v) {
                    return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
                }
            },
            {
                bind:{text: '{Updatedate}'},
                dataIndex: 'updatedate',
                width: 155,
                renderer: function (v) {
                    return Ext.Date.format(new Date(v), 'Y-m-d H:i:s')
                }
            },
            {
                xtype: 'actioncolumn',
                align: 'center',
                bind:{text: '{DeleteName}'},
                width: 100,
                menuDisabled: true,
                items: [{
                    iconCls: 'x-fa fa-trash',
                    bind:{tooltip: '{DeleteName}'},
                    handler: function (view, rowIndex, cellIndex, item, e, record, row) {
            
                        var rs = record;
                        alert(record.get(1))
                        var selModel = view.getSelectionModel();
                        var store = view.getStore();
                        Ext.MessageBox.confirm("{DeleteName}", "{DeleteMessage}",
                            function (btn, value) {
                                if (btn == 'yes') {
                                    Ext.Ajax.request({
                                        url: '/tab/call/RemoveHolidays',
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        params: {
                                            callholidayid: rs.get('callholidayid')
                                        },
                                        success: function (response, options) {
                                            var obj = Ext.decode(response.responseText);
                                            if (obj.success) {
                                                store.remove(rs);
                                                if (store.getCount() > 0) {
                                                    selModel.select(0);
                                                }
                                            } else {
                                                Ext.Msg.alert("Error", obj.msg);
                                            }
                                        },
                                        failure: function (response, opts) {
                                            console.log('server-side failure with status code ' + response.status);
                                        }
                                    });
                                }
                            }
                        );
                    }
                }]
            }
        ],
        dockedItems: [{
            xtype: 'pagingtoolbar',
            dock: 'bottom',
            pageSize: 10,
            displayInfo: true,
            displayMsg: 'Display data from {0} to {1}, a total of {2}',
            emptyMsg: 'No records',
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
        viewConfig: {
            loadMask: true,
            enableTextSelection: true
        }
    }]
});
