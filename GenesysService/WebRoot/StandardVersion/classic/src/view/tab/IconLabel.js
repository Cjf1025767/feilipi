Ext.define('Tab.view.tab.iconlabel', {
    extend:'Ext.form.Label',
    alias: 'widget.iconlabel',
    iconCls: null,
    constructor: function(config) {
		var me = this;
        me.componentCls= config.iconCls ? config.iconCls + ' ' + 'x-label-icon' : null
        me.callParent( arguments );
    }
});