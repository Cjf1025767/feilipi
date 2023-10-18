Ext.define('Tab.view.report.tongkuai.no_answer_callsView', {
    extend: 'Ext.grid.Panel',
    xtype: 'no_answered_call',
    requires: [
        'Tab.view.report.feilipu.no_answer_callsModel',
        'Tab.view.report.feilipu.no_answer_callsStore',
        'Ext.form.field.Spinner'
    ],
    store: Ext.create('Tab.view.report.feilipu.no_answer_callsStore'),
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

    columns: [{
            xtype: 'rownumberer',
            width: 80,//租赁
            align: 'center'
        },

        
        {
            header: "Start Time",align:'center',width: 150, dataIndex: 'start_date_time_string',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "End Time",align:'center',width: 150, dataIndex: 'end_date_time_string',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "Dialing TIme",align:'center',width: 150, dataIndex: 'dialing_time',
            renderer: function (value, cell, records) {
               return  globalVars.reverttime(value);
            }
        },
        {
            header: "Called name",align:'center',width: 150, dataIndex: 'target_address',
        },
        {
            header: "Called Number",align:'center',width: 150, dataIndex: 'target_address',
        },
        {
            header: "Note",align:'center',width: 150, dataIndex: 'target_address',
        },
        {
            header: "Result",align:'center',width: 150, dataIndex: 'technical_result',
        },
         
    ],
    viewConfig: {
        loadMask: true,
        enableTextSelection: true
    }
});