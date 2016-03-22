
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDomMix       from "../main/vdom-mix"

import Paper             from 'material-ui/lib/paper'
import Table             from 'material-ui/lib/Table/table'
import TableHeader       from 'material-ui/lib/Table/table-header'
import TableBody         from 'material-ui/lib/Table/table-body'
import TableHeaderColumn from 'material-ui/lib/Table/table-header-column'
import TableRow          from 'material-ui/lib/Table/table-row'
import TableRowColumn    from 'material-ui/lib/Table/table-row-column'
import IconButton        from 'material-ui/lib/icon-button'
import IconContentAdd    from 'material-ui/lib/svg-icons/content/add'
import IconContentClear  from 'material-ui/lib/svg-icons/content/clear'
import IconContentFilterList from 'material-ui/lib/svg-icons/content/filter-list'
import IconContentRemove from 'material-ui/lib/svg-icons/content/remove'
//Create, Colors, Divider, Helmet, tap-event, StickyToolbars?
import MaterialChip      from './material-chip'


function fail(data){ alert(data) }

const feedback = Feedback()
const vdom = VDomMix(feedback)
const receivers = [feedback.receivers, vdom.receivers, {fail}]
SSEConnection("http://localhost:5556/sse", receivers, 5)

const tp = ({
    Paper,
    Table,TableHeader,TableBody,TableHeaderColumn,TableRow,TableRowColumn,
    IconButton,
    IconContentAdd,IconContentClear,IconContentFilterList,IconContentRemove,
    MaterialChip
})
const transform = ({tp})
vdom.transformBy({transform})