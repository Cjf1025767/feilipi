/*
 * This file is responsible for launching the application. Application logic should be
 * placed in the Rbac.Application class.
 */
Ext.application({
    name: 'Rbac',

    extend: 'Rbac.Application',

    // Simply require all classes in the application. This is sufficient to ensure
    // that all Rbac classes will be included in the application build. If classes
    // have specific requirements on each other, you may need to still require them
    // explicitly.
    //
    requires: [
        'Rbac.*'
    ]
});
