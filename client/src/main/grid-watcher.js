import React from 'react'
// import ReactDOM from 'react-dom'
// import Paper from 'material-ui/lib/paper'
import update          from 'react/lib/update'

const FlexGrid = React.createClass({
    /*componentDidMount(){

    },*/
    render(){
        const pStyle={
            display:"flex",
            flexWrap:"wrap",
            maxWidth:this.props.maxWidth||"100%",
            border:"0px solid black",
            justifyContent:"center",
            //transition:"all 500ms ease",
            position:"relative",
            margin:"0px auto"
        }
        return React.createElement("div",{ key:this.props.key,style:pStyle }, this.props.children)
    }
})

const FlexGridShItem = React.createClass({
    render(){
        const cStyle={
            flex:"1 1 "+this.props.flexBasis,
            border:"0px solid blue",
            maxWidth:(this.props.maxWidth),
            boxSizing:"border-box",
            margin:"0px 5px",
            //transition:"all 200ms ease-out",
            height:(this.props.height||0)+"px"
        }
        return React.createElement("div",{ key:this.props.key, style:cStyle }, this.props.children)
    }
})

const FlexGridItem = React.createClass({
    /*componentWillReceiveProps(nextProps){
        console.log(nextProps)
    },*/
    render(){
        const style={
            position:"absolute",
            top:this.props.y||"0",
            left:this.props.x||"0",
            transition:this.props.noanim?"all 50ms ease-out":"all 300ms ease-out",
            boxSizing:"border-box",
            border:"0px solid black",
            width:(this.props.width||0)+"px",
            //height:this.props.height||"",
            textAlign:this.props.align||"left",
        }
        //console.log(style)
        /*const el=(this.props.paper||0)!=0?Paper:"div",
            ref:(ref)=>
            res(ref,this.props.childOfGrid||null,this.props.gridName||null)*/
        //?[this.props.children[0]]
        return React.createElement("div",{ key: this.props.key, style: style }, this.props.children) 
    }
})

export default function GridWatcher(setLocalState){
    var props_tree={}
    const ref_collection = {}
    function layoutIteration(){
        const refs = Object.values(ref_collection)
        refs.forEach(refc=>{
            const drect = refc.element.getBoundingClientRect()
            refc.width = drect.width
            refc.height = drect.height
            refc.x = drect.left
            refc.y = drect.top
        })
        var diff = DiffPrepare(props_tree)
        refs.forEach(refc=>{
            if(refc.ctx.value !== "FlexGridItem") return;
            const shadow = ref_collection[refc.parent_path_str]
            if(!shadow) return;
            const grid = ref_collection[shadow.parent_path_str]
            if(!grid) return;
            diff.jump(shadow.path)
            diff.addIfChanged("height", refc.height.toFixed(2))
            diff.jump(refc.path)
            diff.addIfChanged("width", shadow.width.toFixed(2))
            diff.addIfChanged("x", (shadow.x-grid.x).toFixed(2))
            diff.addIfChanged("y", (shadow.y-grid.y).toFixed(2))
        })
        const diff_res = diff.getDiff()
        if(diff_res){
            props_tree = update(props_tree,diff_res)
            setLocalState(props_tree)
        }
    }
    const ref = ctx => {
        const path = ctxToPath(ctx)
        const path_str = path.join("/")
        const parent_path = path.slice(0)
        parent_path.splice(-2,1)
        const parent_path_str = parent_path.join("/")
        return element => {
            if(element) ref_collection[path_str] = {ctx,path,path_str,parent_path_str,element} 
            else delete ref_collection[path_str]
        }
    }
    setInterval(layoutIteration, 50)
    const transforms = {ref}
    const componentClasses = ({FlexGrid,FlexGridItem,FlexGridShItem})
    return ({transforms,componentClasses})
}

function ctxToArray(ctx,res){ 
    if(ctx){
        ctxToArray(ctx.parent)
        if(ctx.key) res.push(ctx.key)
    }
    return res
}

function DiffPrepare(imm_tree){
    function getDeepNode(branch, path){
        for(var pos=0; pos<path.length && branch; pos++) branch = branch[path[pos]]
        return branch;
    }
    function getOrCreateNext(branch, key){
        if(!branch[key]) branch[key] = {}
        return branch[key]
    }
    var diff, path, current_imm_node
    function jump(arg_path){
        path = path
        current_imm_node = getDeepNode(imm_tree, path) 
    }
    function addIfChanged(key, value){
        if(current_imm_node[key]===value) return;
        if(!diff) diff = {}
        var imm_branch = imm_tree
        var diff_branch = diff
        for(var pos=0; pos<path.length; pos++) {
            diff_branch = getOrCreateNext(diff_branch, path[pos])
            if(imm_branch){
                imm_branch = imm_branch[path[pos]]
                if(!imm_branch) diff_branch = getOrCreateNext(diff_branch, "$set")
            }
        }
        diff_branch[key] = imm_branch ? value : {"$set":value}
    }
    function getDiff(){ return diff }
    return ({jump,addIfChanged,getDiff})
}
