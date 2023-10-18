Ext.define('Tab.view.report.tongkuai.miss_callsView', {
    extend: 'Ext.grid.Panel',
    xtype: 'miss_calls',
    requires: [
        'Tab.view.report.feilipu.miss_callsModel',
        'Tab.view.report.feilipu.miss_callsStore',
        'Ext.form.field.Spinner'
    ],
    store: Ext.create('Tab.view.report.feilipu.miss_callsStore'),
    listeners: {
        beforerender: function (grid) {
            if( globalVars.workspaceAgent){
            }
        },
        afterrender:function (grid) {
            grid.store.loadPage(1);
        },
    },
    dockedItems: [{
        xtype: 'pagingtoolbar',
        dock: 'bottom',
        pageSize: 10,
        displayInfo: true,
        displayMsg: 'Display data from {0} to {1}, a total of {2}',
        emptyMsg: 'No record',
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
            },
        }
    }],
    // tbar: {
    //     autoScroll: true,
    //     items: [{
    //         xtype: 'container',
    //         layout: 'vbox',
    //         cls:'fix-search-btn',
    //         items: [{
    //                 xtype: 'label',
    //                 style: {
    //                     'margin':'7px',
    //                     fontSize:'16px',
    //                     marginBottom: '10px'
    //                 },
    //                 text: 'Cdr Voice'
    //             },{
    //                 xtype: 'button',
    //                 width: 100,
    //                 margin:'0 0 10 8',
    //                 text: 'Query',
    //                 listeners: {
    //                     'click': function () {
    //                         var grid = this.up().up().up();
    //                         grid.store.loadPage(1);
    //                     }
    //                 },
    //             },
                
    //         ]
        
    //     }]
    // },
    columns: [{
            xtype: 'rownumberer',
            width: 80,//租赁
            align: 'center'
        },

        
         {
            header: "start time",align:'center',width: 150, dataIndex: 'start_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "end time",align:'center',width: 150, dataIndex: 'end_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value)
           }
        },
        {
            header: "duration",align:'center',width: 150, dataIndex: 'duration',
        },
         {
            header: "phonenumber",
            width: 150,
            dataIndex: 'phonenumber',
            align: 'center'
        },
        {
            header: "agent_id",
            width: 150,
            dataIndex: 'agent_id',
            align: 'center'
        },
        {
            header: "disposition",
            width: 150,
            dataIndex: 'disposition',
            align: 'center'
        },
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});