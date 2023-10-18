Ext.define('Tab.view.newreport.reportAgentInOutController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.newreport-reportagentinout',
    
    requires: [
        'Ext.exporter.text.CSV'
    ],
    
    init: function (view) {
        this.groupingFeature = view.view.findFeature('grouping');
    },

    onClearGroupingClick: function () {
        this.groupingFeature.disable();
    },

    onCollapseAll: function () {
        this.groupingFeature.collapseAll();
    },

    onExpandAll: function () {
        this.groupingFeature.expandAll();
    },
    

    onToggleGroup: function (item) {
        this.groupingFeature[item.checked ? 'expand' : 'collapse'](item.text, {
            highlight: true
        });
    },

    onGroupCollapse: function (v, n, groupName) {
        this.syncGroup(groupName, false);
    },

    onGroupExpand: function (v, n, groupName) {
        this.syncGroup(groupName, true);
    },

    syncGroup: function (groupName, state) {
        var groupsBtn = this.lookup('groupsBtn'),
            items = groupsBtn.menu.items.items,
            i;

        for (i = items.length; i-- > 0; ) {
            if (items[i].text === groupName) {
                items[i].setChecked(state, true);
                break;
            }
        }
    },
    
    exportTo: function(btn){
        var cfg = Ext.merge({
            fileName: '坐席通话明细' + '.' + (btn.cfg.ext || btn.cfg.type)
        }, btn.cfg);

        this.getView().saveDocumentAs(cfg);
    },

    onBeforeDocumentSave: function(view){
        this.timeStarted = Date.now();
        view.mask('Document is prepared for export. Please wait ...');
    },

    onDocumentSave: function(view){
        view.unmask();
        Ext.log('export finished; time passed = ' + (Date.now() - this.timeStarted));
    },

    onDataReady: function(){
        Ext.log('data ready; time passed = ' + (Date.now() - this.timeStarted));
    }
});
