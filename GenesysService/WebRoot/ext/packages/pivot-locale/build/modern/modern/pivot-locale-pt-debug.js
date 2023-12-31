Ext.define('Ext.locale.pt.pivot.Aggregators', {
    override: 'Ext.pivot.Aggregators',

    customText:                 'Custom',
    sumText:                    'Soma',
    avgText:                    'Média',
    countText:                  'Contagem',
    minText:                    'Min',
    maxText:                    'Max',
    groupSumPercentageText:     'Grupo percentual Soma',
    groupCountPercentageText:   'Grupo percentual Contagem',
    varianceText:               'Var',
    variancePText:              'Varp',
    stdDevText:                 'StdDev',
    stdDevPText:                'StdDevp'
});
/**
 * Portuguese (Brasil) translation by Rivaldo C Carvalho.
 *
 */

Ext.define('Ext.locale.pt.pivot.Grid', {
    override: 'Ext.pivot.Grid',

    textTotalTpl:       'Total ({name})',
    textGrandTotalTpl:  'Total Geral'
});
Ext.define('Ext.locale.pt.pivot.plugin.DrillDown', {
    override: 'Ext.pivot.plugin.DrillDown',

    titleText: 'Perfurar',
    doneText: 'Feito'
});
Ext.define('Ext.locale.pt.pivot.plugin.configurator.Form', {
    override: 'Ext.pivot.plugin.configurator.Form',

    okText:                     'Ok',
    cancelText:                 'Cancelar',
    formatText:                 'Formatar como',
    summarizeByText:            'Resumir por',
    customNameText:             'Nome personalizado',
    sourceNameText:             'O nome da fonte para este campo é "{form.dataIndex}"',
    sortText:                   'Ordenar',
    filterText:                 'Filtro',
    sortResultsText:            'Ordenar resultados',
    alignText:                  'Alinhar',
    alignLeftText:              'Esquerda',
    alignCenterText:            'Centro',
    alignRightText:             'Certo',

    caseSensitiveText:          'Maiúsculas e minúsculas',
    valueText:                  'Valor',
    fromText:                   'A partir de',
    toText:                     'Para',
    labelFilterText:            'Mostrar itens para os quais o rótulo',
    valueFilterText:            'Mostrar itens para os quais',
    top10FilterText:            'exposição',

    sortAscText:                'Ordenar de A a Z',
    sortDescText:               'Ordenar Z para A',
    sortClearText:              'Desabilita Classificação',
    clearFilterText:            'Desativar filtragem',
    labelFiltersText:           'Filtros de etiquetas',
    valueFiltersText:           'Filtros de valor',
    top10FiltersText:           '10 melhores filtros',

    equalsLText:                'Igual',
    doesNotEqualLText:          'Não é igual',
    beginsWithLText:            'Começa com',
    doesNotBeginWithLText:      'Não começa com',
    endsWithLText:              'Termina com',
    doesNotEndWithLText:        'Não termina com',
    containsLText:              'Contém',
    doesNotContainLText:        'Não contem',
    greaterThanLText:           'É maior que',
    greaterThanOrEqualToLText:  'É maior ou igual a',
    lessThanLText:              'É menor que',
    lessThanOrEqualToLText:     'É menor ou igual a',
    betweenLText:               'Está entre',
    notBetweenLText:            'Não esta entre',
    top10LText:                 'Top 10...',
    topOrderTopText:            'Para cima',
    topOrderBottomText:         'Para baixo',
    topTypeItemsText:           'Items',
    topTypePercentText:         'Percentual',
    topTypeSumText:             'Soma',
 
    requiredFieldText: 'Este campo é necessário',
    operatorText: 'Operador',
    dimensionText: 'Dimensão',
    orderText: 'Ordem',
    typeText: 'Tipo'
});
Ext.define('Ext.locale.pt.pivot.plugin.configurator.Panel', {
    override: 'Ext.pivot.plugin.configurator.Panel',

    panelTitle:             'Configuração',
    cancelText:             'Cancelar',
    okText:                 'Feito',

    panelAllFieldsText:     'Deixe aqui os Campos não utilizados',
    panelTopFieldsText:     'Deixe aqui os Campos das Colunas',
    panelLeftFieldsText:    'Deixe aqui os Campos das Linhas',
    panelAggFieldsText:     'Deixe aqui os Campos de Valores',
    panelAllFieldsTitle:    'Todos os campos',
    panelTopFieldsTitle:    'Rótulos de coluna',
    panelLeftFieldsTitle:   'Rótulos de linha',
    panelAggFieldsTitle:    'Valores'
});

Ext.define('Ext.locale.pt.pivot.plugin.configurator.Settings', {
    override: 'Ext.pivot.plugin.configurator.Settings',

    titleText: 'Configurações',
    okText: 'Ok',
    cancelText: 'Cancelar',
    layoutText: 'Layout',
    outlineLayoutText: 'Esboço',
    compactLayoutText: 'Compactar',
    tabularLayoutText: 'Tabular',
    firstPositionText: 'Primeiro',
    hidePositionText: 'Ocultar',
    lastPositionText: 'Último',
    rowSubTotalPositionText: 'Posição subtotal da linha',
    columnSubTotalPositionText: 'Posição subtotal da coluna',
    rowTotalPositionText: 'Posição total da linha',
    columnTotalPositionText: 'Posição total da coluna',
    showZeroAsBlankText: 'Mostrar zero como em branco',
    yesText: 'Sim',
    noText: 'Não'
});
Ext.define('Ext.locale.pt.pivot.plugin.rangeeditor.Panel', {
    override: 'Ext.pivot.plugin.rangeeditor.Panel',

    titleText:      'Range editor',
    valueText:      'Valor',
    fieldText:      'Campo de origem é "{form.dataIndex}"',
    typeText:       'Tipo',
    okText:         'Ok',
    cancelText:     'Cancela'
});
