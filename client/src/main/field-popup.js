import React from 'react'

const FieldPopupBox= React.createClass({
   render(){
      const style={
          display:"block",
          width:"100%",
          //position:"relative",
         // zIndex:4000
      }
      const ref = el => this.props.popupReg(false,el)
      const hrStyle={
        width:"100%",
        position:"absolute",
        bottom:"8px",
        borderWidth:"medium medium 1px",
        borderStyle:"none none solid",
        borderColor:"rgb(224,224,224)",
        boxSizing:"content-box",
        margin:"0px",
        display:this.props.showUnderscore?"":"none"
      }
      return React.createElement("div",{key:"fieldpopupbox",style,ref},[this.props.children,React.createElement("hr",{key:"underscore",style:hrStyle})])
   }
})

const FieldPopupDrop = React.createClass({
       render(){
        var dropBoxTop=this.props.top?this.props.top:0
        var dropBoxLeft=this.props.left?this.props.left:0

        const style={
            maxHeight:this.props.showDrop?this.props.maxHeight?this.props.maxHeight:"300px":"0px",
            visibility:this.props.visibility?this.props.visibility:"hidden",
            display:"inline-block",
            position:"fixed",
            overflow:"hidden",
            //minWidth:"100%",
            //overflowY:"auto",
            padding:"0px 2px 0px 2px",
            top:dropBoxTop===0?"":dropBoxTop+"px",
            left:dropBoxLeft===0?"":dropBoxLeft+"px",
            transition:"max-height 200ms,top 50ms",
            zIndex:2100,
            boxSizing:"border-box",
            color:"rgba(1,1,1,0.87)",
            backgroundColor:"rgb(255,255,255)",
            boxShadow:"0px 1px 6px rgba(0,0,0,0.12),0px 1px 4px rgba(0,0,0,0.12)"
        }
        const ref = el => this.props.popupReg(true,el)
        return React.createElement("div",{key:"fieldpopupdrop",style,ref},this.props.children)
    }  
	
})
const preset={}
preset.positions={
    TOP:0,
    BOTTOM:1,
    LEFT:2,
    RIGHT:3,
    CENTER:4
}
export default function FieldPopup(vDom,DiffPrepare){
    var refCollection={}
    var interval=null
    function popupListener(){
        const refs = Object.keys(refCollection).map(key=>refCollection[key])
        const popupBoxes=refs.filter(x=>!x.isPopup)

        var diff = DiffPrepare(vDom.localState)
        popupBoxes.forEach((popupBox)=>{
            const popupDrop=refs.filter(x=>x.parent_path_str===popupBox.parent_path_str&&x.isPopup)[0]
            popupBox.drect=popupBox.element.getBoundingClientRect()
            popupDrop.drect=popupDrop.element.getBoundingClientRect()


            diff.jump(popupDrop.path)
            var newTop=0
            var newLeft=0

            var position=preset.positions.BOTTOM;

            if(popupDrop.drect.height!==0){
               // console.log("popupbox",popupBox.drect,"popupdrop",popupDrop.drect);


                if(window.innerHeight<=popupBox.drect.bottom+popupDrop.drect.height){    //case no space at the bottom;
                    position=preset.positions.TOP
                    if(popupDrop.drect.height>popupBox.drect.top){
                        position=preset.positions.LEFT
                        if(popupDrop.drect.right>popupBox.drect.left){
                            position=preset.positions.RIGHT
                            if(window.innerWidth<=popupBox.drect.right+popupDrop.drect.width)
                                position=preset.positions.CENTER
                        }
                    }
                }

                switch(position){
                    case preset.positions.BOTTOM :
                        newTop = popupBox.drect.bottom
                        newLeft = popupBox.drect.left
                        break;
                    case preset.positions.TOP :
                        newTop = popupBox.drect.top - popupDrop.drect.height
                        newLeft = popupBox.drect.left
                        break;
                    case preset.positions.LEFT :
                        newTop = window.innerHeight/2 - popupDrop.drect.height/2
                        newLeft = popupBox.drect.left - popupDrop.drect.width
                        break
                    case preset.positions.RIGHT :
                        newTop = window.innerHeight/2 - popupDrop.drect.height/2
                        newLeft = popupBox.drect.right
                        break
                    case preset.positions.CENTER :
                        newTop = window.innerHeight/2 - popupDrop.drect.height/2
                        newLeft = window.innerWidth/2 - popupDrop.drect.width/2
                        break;
                    default: break;
                }

                //console.log(position,newTop,newLeft)
                diff.addIfChanged("top",newTop.toFixed(2))
                diff.addIfChanged("left",newLeft.toFixed(2))
                diff.addIfChanged("visibility","visible")
            }
        })
        diff.apply()

        clearInterval(interval)
        interval=null
    }
    function parseReg(ctx){
        const path = vDom.ctxToArray(ctx,[])

        const path_str = path.join("/")
        const parent_path = path.slice(0)
        parent_path.splice(-2,1)
        const parent_path_str = parent_path.join("/")

	    return function reg(isPopup,element){
            if(element) refCollection[path_str] = {path,parent_path_str,element,isPopup}
            else delete refCollection[path_str]
            setTimeout(()=>{
                if(interval===null){
                    interval=setInterval(popupListener,50)
                }
            },300)
	    }
    }

    const popupReg = {"def":parseReg}
    const tp = {FieldPopupBox,FieldPopupDrop}
    const transforms ={popupReg,tp}

    return ({transforms})
}