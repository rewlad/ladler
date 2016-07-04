"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDomMix       from "../main/vdom-mix"

import React             from 'react'
import ReactDOM          from 'react-dom'

import Paper             from 'material-ui/Paper'
import RaisedButton      from 'material-ui/RaisedButton'
import IconEditorModeEdit from 'material-ui/svg-icons/editor/mode-edit'
import IconContentAdd    from 'material-ui/svg-icons/content/add'
import IconContentClear  from 'material-ui/svg-icons/content/clear'
import IconNavigationClose from 'material-ui/svg-icons/navigation/close'
import IconContentSave  from 'material-ui/svg-icons/content/save'
import IconActionDelete  from 'material-ui/svg-icons/action/delete'
import IconActionRestore from 'material-ui/svg-icons/action/restore'
import IconActionLock from 'material-ui/svg-icons/action/lock'
import IconActionDateRange from 'material-ui/svg-icons/action/date-range'
import IconActionSchedule from 'material-ui/svg-icons/action/schedule'
import IconContentFilterList from 'material-ui/svg-icons/content/filter-list'
import IconContentRemove from 'material-ui/svg-icons/content/remove'
import IconSocialPerson  from 'material-ui/svg-icons/social/person'
import IconNavigationMenu from 'material-ui/svg-icons/navigation/menu'
import IconNavigationDropDown from 'material-ui/svg-icons/navigation/arrow-drop-down'
import IconNavigationDropUp from 'material-ui/svg-icons/navigation/arrow-drop-up'
import IconNavigationExpandMore from 'material-ui/svg-icons/navigation/expand-more'
import IconNavigationExpandLess from 'material-ui/svg-icons/navigation/expand-less'
import TextField         from 'material-ui/TextField/TextField'
import Checkbox          from 'material-ui/Checkbox'
import MaterialChip      from '../main/material-chip'
import {CrazyCalendar,CrazyClock} from '../main/material-chronos'
import injectTapEventPlugin from "react-tap-event-plugin"
import Helmet            from 'react-helmet'
import {MuiThemeParent}    from '../main/material-utils'
import {DataTableRow}      from '../main/flex-data-table'
import {KeyboardReceiver,CursorOver}    from '../main/common-utils'
import {LabeledText,SnackBarEx,IconButtonEx}  from '../main/material-extensions'

injectTapEventPlugin()
function fixOnScrollBug(){
    document.body.style.overflowY="scroll"
}
fixOnScrollBug()
function fail(data){ alert(data) }

const feedback = Feedback()
const vdom = VDomMix(feedback)
const receivers = [feedback.receivers, vdom.receivers, {fail}]
SSEConnection("http://localhost:5556/sse", receivers, 5)

const tp = ({
    Paper,
    RaisedButton,
    IconButtonEx,
    IconEditorModeEdit,MaterialChip,
    IconContentAdd,IconContentClear,IconContentFilterList,IconContentRemove,IconActionDelete,
    TextField,Checkbox,
    DataTableRow,
    LabeledText,
    IconActionLock,IconSocialPerson,
    IconActionRestore,IconContentSave,
    CrazyCalendar,CrazyClock,
    IconNavigationMenu,
    CursorOver,
    IconNavigationDropDown,IconNavigationDropUp,IconActionDateRange,IconNavigationExpandMore,IconNavigationExpandLess,
    IconActionSchedule, IconNavigationClose, Helmet,
    SnackBarEx,KeyboardReceiver,
    MuiThemeParent
})

const transforms = ({tp})

vdom.transformBy({transforms})