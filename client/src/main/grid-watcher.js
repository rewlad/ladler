import React from 'react'
import ReactDOM from 'react-dom'
import Paper from 'material-ui/lib/paper'
import update          from 'react/lib/update'

const FlexGrid=React.createClass({
    componentDidMount(){

    },
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
        if((this.props.style||0)!=0){
            Object.assign(pStyle,this.props.style)
        }
        if((this.props.childOfGrid||0)!=0){
            Object.assign(pStyle,{
                position:"absolute",
                top:this.props.y||"0",
                left:this.props.x||"0",
                //height:this.props.height||"",
                //transition:"all 500ms ease",
                boxSizing:"border-box",
                border:"1px solid black",
                width:this.props.width||"100%"
            })
        }


        const el=(this.props.paper||0)==0?"div":Paper

        return React.createElement(el,{key:this.props.key,style:pStyle/*,ref:(ref)=>
            res(ref,this.props.childOfGrid||null,this.props.gridName)*/},this.props.children)
    }

})
const FlexGridShItem=React.createClass({

    render(){
        const cStyle={
            flex:"1 1 "+(this.props.flexBasis||"300px"),

            border:"0px solid blue",

            maxWidth:(this.props.maxWidth||"300px"),
            boxSizing:"border-box",
            margin:"0px 5px",
            //transition:"all 200ms ease-out",
            height:(this.props.height||0)+"px"

        }

        return React.createElement("div",{key:this.props.key,style:cStyle})

    }
})
const FlexGridItem=React.createClass({
    componentWillReceiveProps(nextProps){
        //console.log(nextProps)
    },
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
        const el=(this.props.paper||0)!=0?Paper:"div"
        return React.createElement(el,{key:this.props.key,style:style/*,ref:(ref)=>
            res(ref,this.props.childOfGrid||null,this.props.gridName||null)*/},[this.props.children[0]])
    }
})




export default function GridWatcher(setLocalState){


/*
function observer()
{

    const pGrids=ref_collection.filter(x=>x.gridName!=null)
    const t=Array(1).fill(0)
    t.forEach((z)=>{
    pGrids.forEach(g=>{
         const children=ref_collection.filter(x=>x.childOfGrid==g.gridName)
            children.forEach((child,i)=>{
                const sc_rect=g.ref.children[i].getBoundingClientRect()
                const child_rect=child.ref.getBoundingClientRect()
                g.ref.children[i].style.height=child_rect.height+"px"
                const newTop = sc_rect.top+window.pageYOffset
                const newLeft = sc_rect.left+window.pageXOffset

                if (child_rect.top != newTop || child_rect.left != newLeft ||
                    sc_rect.width != child_rect.width) {

                    const newTransition = child_rect.top==newTop && Math.abs(child_rect.left-newLeft) < 50 ?
                        "none":"all 300ms ease-out"
                    if (child.ref.style.transition != newTransition)
                        child.ref.style.transition = newTransition

                    child.ref.style.width = sc_rect.width+"px"
                    child.ref.style.top   = newTop+"px"
                    child.ref.style.left  = newLeft+"px"
                }
            })

    })

    })

    setTimeout(observer,50)
   // ref_collection=[]
}
*/
const ref_collection=[]
const grid_obj_collection=[]

function MergeRecursive(obj1, obj2) {

  for (var p in obj2) {

      // Property in destination object set; update its value.
      if ( obj2[p].constructor==Object ) {
        if(obj1[p]!==undefined)
            obj1[p] = MergeRecursive(obj1[p], obj2[p]);
        else
            obj1[p]=obj2[p]
      } else {

        obj1[p] = obj2[p];


      }


  }

  return obj1;
}




function get_childrenOf(obj,other){
    const obj_path_string=obj.path.toString()


    const shc=other.filter(o=>{
       return o.path.slice(0,-1).toString()===obj_path_string

    })

    const c=other.filter(o=>{

        return shc.filter(sc=>{
            const tmp=sc.path.slice(0);
            tmp.splice(-2,1)
            return o.path.toString()===tmp.toString()
        }).length>0
    })
    return ({shc,c})
}


function build_deep(obj,flag=false){

    const rarr=obj.path.slice(0).reverse()

    var t_init
    if(flag)
        t_init={"at":{x:{$set:obj.x},y:{$set:obj.y},width:{$set:obj.width}/*,anim:obj.anim*/,height:{$set:obj.height}}}
    else
        t_init={"at":{x:obj.x,y:obj.y,width:obj.width/*,anim:obj.anim*/,height:obj.height}}


    rarr.forEach((r,i)=>{

        const wrap={}

        wrap[r]=t_init
        t_init=wrap
    })

    return t_init;
}
function build_props_tree(arr){
    const ret={}
    arr.forEach(a=>{
      const ret1=build_deep(a)

       MergeRecursive(ret,ret1)
    })

    return ret
}
function equalCtx(a,b){
    const keyA=Object.keys(a)[0]
    const keyB=Object.keys(b)[0]

    if(keyA==="at"&&keyB==="at") return true
    if(keyA===keyB)  return equalCtx(a[keyA],b[keyB])

    return false
}
function indexOfCtx(refa,ctx){
    var index=-1;
    const cp=build_path(ctx)
    refa.forEach((ra,i)=>{
      if(index>-1) return
      const bp=build_path(ra.ctx)
      if(cp.length!==bp.length) return
      if(bp.every((x,i)=>x===cp[i])) index=i
    })

    return index;
}
function indexOfPath(nv,path){
    var index=-1;
    nv.forEach((n,i)=>{
        if(index>-1) return

        if(n.path.length!==path.length) return

        if(n.path.every((x,j)=>x===path[j])) index=i
    })

    return index
}

var props_tree={}
function remix(){

    const grids=grid_obj_collection.filter(x=>x.grid)

    grids.forEach(g=>{

        const children=get_childrenOf(g,grid_obj_collection)

        children.shc.forEach(sc=>{

            const sc_path_length=sc.path.length

            children.c.forEach(c=>{
                if(sc.path[sc_path_length-1]===c.path[c.path.length-1]){
                    sc.height=c.height
                    c.width=sc.width
                    c.x=sc.x
                    c.y=sc.y
                }
            })
        })
    })
  //  console.log(grid_obj_collection)
    var changed=0
    if(Object.keys(props_tree).length===0||grid_obj_collection_length!==grid_obj_collection.length){
        const props=build_props_tree(grid_obj_collection)
        props_tree=update(props_tree,{$set:props})
        changed=1
     //   console.log("rebuild")
    }
    else
    {
        //console.log(grid_obj_collection)
        const props_array=grid_obj_collection.map(x=>build_deep(x,true))
        props_array.forEach(p=>{
           const cmp_res=cmp_att(props_tree,p)
            if(cmp_res){
                props_tree=update(props_tree,p)
                changed=1;
            }
        })
      //  console.log("mixed set")
    }

   if(changed)
   {
    grid_obj_collection_length=grid_obj_collection.length
    //console.log(grid_obj_collection.length,":",grid_obj_collection)
  //  console.log("update")
    setLocalState(props_tree)

   }


}

const ref=ctx=>element=>{

    const index=indexOfCtx(ref_collection,ctx)
    switch(ctx.value){
        case("observer()"):
            build_grid_obj_collection()
            break;
        case("grid"):
            if(index>-1)
                ref_collection[index]={element,grid:true,ctx}
            else
                ref_collection.push({element,grid:true,ctx})
        break;
        case("childofgrid"):
            if(index>-1)
                ref_collection[index]={element,grid:false,ctx}
            else
                ref_collection.push({element,grid:false,ctx})
            break;
        case("shchildofgrid"):
            if(index>-1)
                ref_collection[index]={element,grid:false,ctx}
            else
                ref_collection.push({element,grid:false,ctx})
            break;
        default:break;
    }


}
var grid_obj_collection_length=0;
function build_grid_obj_collection(){
       //grid_obj_collection.splice(0,grid_obj_collection.length)

       ref_collection.forEach(refc=>{
            const path=build_path(refc.ctx)

            const index=indexOfPath(grid_obj_collection,path)

            if(refc.element===null) {
                if(index>-1)
                    grid_obj_collection.splice(index,1)
               return
            }
            const dref=ReactDOM.findDOMNode(refc.element);
            if(dref===null)console.log(refc.element)
            const drect=dref.getBoundingClientRect()



            const newWidth=drect.width.toFixed(2)
            const newHeight=drect.height.toFixed(2)
            const newX=(drect.left.toFixed(2)+window.pageXOffset)
            const newY=(drect.top.toFixed(2)+window.pageYOffset)
            if(index>-1)
            {

                grid_obj_collection[index]={path,
                 grid:refc.grid,
                 x:newX,
                 y:newY,
                 width:newWidth,
              //   anim:"true",
                 height:newHeight}
            }
            else
            {

             grid_obj_collection.push({path,
                grid:refc.grid,
                x:newX,
                y:newY,
                width:newWidth,
               // anim:"true",
                height:newHeight})
             }
       })

       remix()


       //window.requestAnimationFrame(build_grid_obj_collection)
       setTimeout(build_grid_obj_collection,50)

}
function build_path(path,res=[]){
    if(!path.parent.parent) return res.reverse()
    if(path.parent.key!=="at"&&path.parent.key!==undefined)
        res.push(path.parent.key)
    return build_path(path.parent,res)
}

    function handleResize(){
      //  build_grid_obj_collection()
    }
    window.addEventListener("resize",handleResize)

    const transforms = {ref}
    const componentClasses = ({FlexGrid,FlexGridItem,FlexGridShItem})
    //return ({observer,FlexGrid,FlexGridItem})
    return ({transforms,componentClasses})
}

function cmp_att(tree,obj)
{
//console.log(tree,obj)
    var diff=0;
    var Obj=obj
    var tObj=tree
    do{
    const objK=Object.keys(Obj)[0]
        //if(tObj===undefined) return -1
        tObj=tObj[objK]
        Obj=Obj[objK]
        if(objK==="at") break;
    }while(1)

    if(tObj["width"]!==Obj["width"].$set) diff=1
    if(tObj["height"]!==Obj["height"].$set) diff=1
    if(tObj["x"]!==Obj["x"].$set) diff=1
    if(tObj["y"]!==Obj["y"].$set) diff=1

    return diff
}