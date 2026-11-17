package com.openminis.app.browser

/**
 * Read-only DOM checks. This cannot replace real screenshots or user testing.
 */
internal object AltivVisualAudit {
    val script = """
        const root=document.documentElement;
        const w=window.innerWidth, h=window.innerHeight;
        const docWidth=Math.max(root.scrollWidth,document.body?.scrollWidth||0);
        const counts={overflow:0,brokenImages:0,pendingImages:0,assetPlaceholders:0,
            hiddenSections:0,tinyText:0,smallTargets:0,lowContrast:0,contrastUnknown:0};
        const samples=[];
        const issue=(kind,e,description)=>{
            counts[kind]++;
            if(samples.length<24)samples.push({kind,tag:e?.tagName?.toLowerCase()||"document",description});
        };
        const visible=e=>{
            const s=getComputedStyle(e), r=e.getBoundingClientRect();
            return s.display!=="none"&&s.visibility!=="hidden"&&Number(s.opacity)>0.01&&r.width>1&&r.height>1;
        };
        if(docWidth>w+3)issue("overflow",root,"document wider than viewport by "+(docWidth-w)+"px");
        for(const e of Array.from(document.querySelectorAll("body *")).slice(0,1200)){
            if(!visible(e))continue;
            const r=e.getBoundingClientRect(),p=e.parentElement;
            if(r.left>=-8&&r.right<=w+8)continue;
            const pr=p?.getBoundingClientRect();
            if(!pr||pr.left>=-8&&pr.right<=w+8)issue("overflow",e,"element exceeds viewport");
        }
        for(const e of Array.from(document.images).slice(0,160)){
            if(!e.complete)issue("pendingImages",e,"image loading");
            else if(e.naturalWidth===0)issue("brokenImages",e,"image failed to decode");
        }
        // Visible fallback placeholders are not valid image assets. The browser's
        // DOM cannot establish whether a successful image depicts the right subject.
        const fallbackText=/^(?:imagem[ ]+)?(?:indispon[ií]vel|sem[ ]+imagem|image[ ]+unavailable|no[ ]+image|image[ ]+not[ ]+found)$/i;
        for(const e of Array.from(document.querySelectorAll("main span,main p,main div,article span,article p,[class*=gallery] span,[class*=showroom] span")).slice(0,900)){
            if(e.children.length||!visible(e))continue;
            const text=(e.textContent||"").trim();
            if(fallbackText.test(text))
                issue("assetPlaceholders",e,"visible missing-media fallback: "+text.slice(0,55));
        }
        for(const e of Array.from(document.querySelectorAll("main section,main article,body>section")).slice(0,60)){
            const r=e.getBoundingClientRect(),s=getComputedStyle(e);
            if(r.height<140)continue;
            if(s.display==="none"||s.visibility==="hidden"||Number(s.opacity)<=.01)
                issue("hiddenSections",e,"large section hidden by CSS");
            else if(r.height>Math.max(450,h*.8)&&e.innerText.trim().length<20&&!e.querySelector("img,video,canvas,svg"))
                issue("hiddenSections",e,"large section with no text or media");
        }
        const rgb=s=>{const m=s.match(/^rgba?\(\s*(\d+)[, ]+\s*(\d+)[, ]+\s*(\d+)/i);return m?[+m[1],+m[2],+m[3]]:null;};
        const lum=a=>a.map(x=>{x/=255;return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);}).reduce((v,x,i)=>v+x*[.2126,.7152,.0722][i],0);
        const ratio=(a,b)=>{const x=lum(a),y=lum(b);return(Math.max(x,y)+.05)/(Math.min(x,y)+.05);};
        const background=e=>{
            let x=e,ambiguous=false;
            while(x&&x.nodeType===1){
                const s=getComputedStyle(x);
                if(s.backgroundImage!=="none")ambiguous=true;
                const c=rgb(s.backgroundColor);
                if(c&&s.backgroundColor!=="rgba(0, 0, 0, 0)")return ambiguous?null:c;
                x=x.parentElement;
            }
            return ambiguous?null:[255,255,255];
        };
        for(const e of Array.from(document.querySelectorAll("h1,h2,h3,p,li,a,button")).slice(0,320)){
            if(!visible(e)||!e.textContent.trim())continue;
            const s=getComputedStyle(e),font=parseFloat(s.fontSize);
            if(e.matches("p,li")&&font<13||e.matches("h1,h2,h3")&&font<16)
                issue("tinyText",e,"font below recommended size");
            const a=rgb(s.color),b=background(e);
            if(!a||!b){counts.contrastUnknown++;continue;}
            const large=font>=24||font>=18.67&&parseInt(s.fontWeight,10)>=700;
            if(ratio(a,b)<(large?3:4.5))issue("lowContrast",e,"approximate CSS contrast below AA");
        }
        for(const e of Array.from(document.querySelectorAll("button,a,input,select,textarea,[role=button]")).slice(0,200)){
            if(!visible(e))continue;
            const r=e.getBoundingClientRect();
            if(r.width<44||r.height<44)issue("smallTargets",e,"touch target below 44px");
        }
        return {kind:"ALTIVE_DOM_AUDIT_V2",
            method:"read-only DOM heuristics, not image-based review",
            viewport:{width:w,height:h,documentWidth:docWidth},
            hasViewportMeta:!!document.querySelector('meta[name="viewport"]'),
            counts,samples,visualApproval:"NOT_VERIFIED",
            semanticImageApproval:"NOT_VERIFIED",screenshotRequired:true};
    """.trimIndent()
}
