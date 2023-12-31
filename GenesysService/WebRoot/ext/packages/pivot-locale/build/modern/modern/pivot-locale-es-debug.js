Ext.define('Ext.locale.es.pivot.Aggregators', {
    override: 'Ext.pivot.Aggregators',

    customText:                 'Custom',
    sumText:                    'Suma',
    avgText:                    'Prom',
    countText:                  'Cont',
    minText:                    'Min',
    maxText:                    'Max',
    groupSumPercentageText:     'Grupo % suma',
    groupCountPercentageText:   'Grupo % cont',
    varianceText:               'Var',
    variancePText:              'Varp',
    stdDevText:                 'StdDev',
    stdDevPText:                'StdDevp'
});
/**
 * Spanish translation by Gustavo Ruiz
 * The original translation was done for version 2.x.
 *
 */

Ext.define('Ext.locale.es.pivot.Grid', {
    override: 'Ext.pivot.Grid',

    textTotalTpl:       'Total ({name})',
    textGrandTotalTpl:  'Total General'
});
Ext.define('Ext.locale.es.pivot.plugin.DrillDown', {
    override: 'Ext.pivot.plugin.DrillDown',

    titleText: 'Profundizar',
    doneText: 'Hecho'
});
Ext.define('Ext.locale.es.pivot.plugin.configurator.Form', {
    override: 'Ext.pivot.plugin.configurator.Form',

    okText:                     'Ok',
    cancelText:                 'Cancelar',
    formatText:                 'Formatear como',
    summarizeByText:            'Resumir por',
    customNameText:             'Nombre personalizado',
    sourceNameText:             'El nombre de la fuente para este campo es "{form.dataIndex}"',
    sortText:                   'Ordenar',
    filterText:                 'Filtrar',
    sortResultsText:            'Ordenar resultados',
    alignText:                  'Alinear',
    alignLeftText:              'Izquierda',
    alignCenterText:            'Centrar',
    alignRightText:             'Derecha',

    caseSensitiveText:          'Distingue mayúsculas y minúsculas',
    valueText:                  'Valor',
    fromText:                   'De',
    toText:                     'A',
    labelFilterText:            'Mostrar los artículos para los cuales la etiqueta',
    valueFilterText:            'Mostrar artículos para los cuales',
    top10FilterText:            'Espectáculo',

    sortAscText:                'Ordenar de la A a la Z',
    sortDescText:               'Ordenar Z a A',
    sortClearText:              'Desactivar clasificación',
    clearFilterText:            'Deshabilitar el filtrado',
    labelFiltersText:           'Filtros de etiquetas',
    valueFiltersText:           'Filtros de valor',
    top10FiltersText:           'Los 10 mejores filtros',

    equalsLText:                'Igual',
    doesNotEqualLText:          'No es igual',
    beginsWithLText:            'Empieza con',
    doesNotBeginWithLText:      'No comienza con',
    endsWithLText:              'Termina con',
    doesNotEndWithLText:        'No termina con',
    containsLText:              'Contiene',
    doesNotContainLText:        'No contiene',
    greaterThanLText:           'Es mayor que',
    greaterThanOrEqualToLText:  'Es mayor o igual a',
    lessThanLText:              'Es menos que',
    lessThanOrEqualToLText:     'Es menor o igual que',
    betweenLText:               'Está entre',
    notBetweenLText:            'No está entre',
    top10LText:                 'Top 10...',
    topOrderTopText:            'Parte superior',
    topOrderBottomText:         'Fondo',
    topTypeItemsText:           'Artículos',
    topTypePercentText:         'Por ciento',
    topTypeSumText:             'Suma',
 
    requiredFieldText: 'Este campo es requerido',
    operatorText: 'Operador',
    dimensionText: 'Dimensión',
    orderText: 'Orden',
    typeText: 'Tipo'
});
Ext.define('Ext.locale.es.pivot.plugin.configurator.Panel', {
    override: 'Ext.pivot.plugin.configurator.Panel',

    panelTitle:             'Configuración',
    cancelText:             'Cancelar',
    okText:                 'Hecho',

    panelAllFieldsText:     'Coloque los Campos no utilizados aquí',
    panelTopFieldsText:     'Coloque las Columnas aquí',
    panelLeftFieldsText:    'Coloque las Filas aquí',
    panelAggFieldsText:     'Coloque los Campos Acumulados aquí',
    panelAllFieldsTitle:    'Todos los campos',
    panelTopFieldsTitle:    'Etiquetas de columna',
    panelLeftFieldsTitle:   'Etiquetas de fila',
    panelAggFieldsTitle:    'Valores'
});

Ext.define('Ext.locale.es.pivot.plugin.configurator.Settings', {
    override: 'Ext.pivot.plugin.configurator.Settings',

    titleText: 'Configuraciones',
    okText: 'Ok',
    cancelText: 'Cancelar',
    layoutText: 'Diseño',
    outlineLayoutText: 'Contorno',
    compactLayoutText: 'Compacto',
    tabularLayoutText: 'Tabular',
    firstPositionText: 'primero',
    hidePositionText: 'Esconder',
    lastPositionText: 'Último',
    rowSubTotalPositionText: 'Posición subtotal de fila',
    columnSubTotalPositionText: 'Posición del subtotal de columna',
    rowTotalPositionText: 'Posición total de la fila',
    columnTotalPositionText: 'Posición total de la columna',
    showZeroAsBlankText: 'Mostrar cero como blanco',
    yesText: 'Sí',
    noText: 'No'
});
Ext.define('Ext.locale.es.pivot.plugin.rangeeditor.Panel', {
    override: 'Ext.pivot.plugin.rangeeditor.Panel',

    titleText:      'Editor de rango',
    valueText:      'Valor',
    fieldText:      'El campo fuente es "{form.dataIndex}"',
    typeText:       'Tipo',
    okText:         'Aceptar',
    cancelText:     'Cancelar'
});
