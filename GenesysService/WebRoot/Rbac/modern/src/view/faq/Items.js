Ext.define('Rbac.view.faq.Items', {
    extend: 'Ext.Panel',
    xtype: 'faqitems',

    requires: [
        'Ext.dataview.DataView'
    ],

    controller: 'faqitems',

    bodyPadding: '0 20 20 20',

    config: {
        store: null
    },

    items: [{
        xtype: 'dataview',
        scrollable: false,

        listeners: {
            itemtap: 'onItemTap'
        },

        itemTpl: '<div class="faq-item">' +
                '<div class="faq-title">' +
                    '<div class="faq-expander x-fa"></div>' +
                    '<div class="faq-question"></div>' +
                '</div>' +
                '<div class="faq-body">' +
                    '<div></div>' +
                '</div>' +
            '</div>'
    }],

    updateStore: function (store) {
        var grid = this.down('dataview');
        grid.setStore(store);
    }
});
