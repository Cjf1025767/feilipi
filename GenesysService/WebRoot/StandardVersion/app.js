/*
 * This file is responsible for launching the application. Application logic should be
 * placed in the Tab.Application class.
 */
Ext.application({
    name: 'Tab',

    extend: 'Tab.Application',

    // Simply require all classes in the application. This is sufficient to ensure
    // that all Tab classes will be included in the application build. If classes
    // have specific requirements on each other, you may need to still require them
    // explicitly.
    //
    requires: [
        'Tab.*'
    ]
});
