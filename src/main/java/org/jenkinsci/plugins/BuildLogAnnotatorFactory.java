package org.jenkinsci.plugins;

import hudson.console.*;
import hudson.model.*;
import hudson.Extension;
import hudson.MarkupText;
import java.util.logging.Logger;


/**
 * 此類別的功能為修改Build log的顯示方式，在log中加入行數。
 * 
 * 主要是透過擴充Jenkins Extension：ConsoleAnnotatorFactory來達成此一功能，
 * BuildLogAnnotatorFactory是一Describable類別，同時也是一Factory pattern，
 * Build log的邏輯其實是實作於ConsoleAnnotator這個子類別中，Factory的責任是負責處理如何
 * 生成(Create)這個實作物件。而BuildLogAnnotatorDescriptor子類別則是負責實作Descriptor，
 * 主要功能是封裝處理global.jelly的資料。
 * 
 * @author Sam Chiu
 */
@Extension
public class BuildLogAnnotatorFactory extends ConsoleAnnotatorFactory<Object> 
                             implements Describable<BuildLogAnnotatorFactory> {

    private static final Logger logger = Logger.getLogger(
    								BuildLogAnnotatorFactory.class.getName());
    
    /**
     * newInstance為生成ConsoleAnnotator的統一接口，也是Factory pattern的主要功能。
     * 這個方法會在當Console output page備產生前執行，這個方法也會被同時執行，也就是
     * 每個Build job執行時都會生成一個ConsoleAnnotator物件來進行Console output的訊息處理。
     * @see hudson.console.ConsoleAnnotatorFactory#newInstance(java.lang.Object)
     */
    @Override
    public ConsoleAnnotator newInstance(Object context) {
    	return new BuildLogConsoleAnnotator(getDescriptor().getEnableLinenumber());
    }

    private static class BuildLogConsoleAnnotator extends ConsoleAnnotator {
        private int linenumber = 0;
        private String bracketNow = null;
        private boolean mEnable;
        public BuildLogConsoleAnnotator(boolean enable) {
        	mEnable = enable;
        }
        /**
         * annotate方法是主要實作如何標記Build log的訊息內容的地方。
         * @see hudson.console.ConsoleAnnotator#annotate(java.lang.Object, 
         * 													hudson.MarkupText)
         */
        public ConsoleAnnotator annotate(Object context, MarkupText text) {
            try {
            	if (mEnable){
                    linenumber++;
                    text.addMarkup(0, String.format("%1$5d ", linenumber) );
            	}
            } catch (Exception e ) {
                logger.warning("annotation is fail : " + e.toString());
            }
            return this;
        }
    }
    
    /*
     * getDescriptor為生成Descriptor的唯一界面，是一種Singleton pattern，也就是說
     * 此BuildLogAnnotatorDescriptor類別在系統上只會有單一個個體。
     * 產生BuildLogAnnotatorDescriptor物件的方式是透過Hudson物件的getDescriptor方法，
     * 並傳入欲生成的類別作為參數。
     * @see hudson.model.Describable#getDescriptor()
     */
    @Override
    public BuildLogAnnotatorDescriptor getDescriptor() {
        return (BuildLogAnnotatorDescriptor)Hudson.getInstance().getDescriptor(getClass());
    }
    
    /**
     * 此類別的功能主要用於擴充Jenkins global configuration頁面，並提供是否啓用
     * Line number的設定。
     */
    @Extension
    public static final class BuildLogAnnotatorDescriptor 
    							extends Descriptor<BuildLogAnnotatorFactory> {
        
		private boolean enable;
		
        public BuildLogAnnotatorDescriptor() {
        	// 在class的建構子呼叫load()可以從硬碟讀取之前透過save()的資料
        	load();
        }

        /**
         * 透過覆寫configure方法來處理global.jelly網頁送出後的資料
         */
        @Override
        public boolean configure(org.kohsuke.stapler.StaplerRequest req,
                    net.sf.json.JSONObject formData) throws FormException {

        	// formData為一JSONObject，Jelly的網頁資料透過此一物件封裝,
        	// "enablelinenumber"為global.jelly中所對應的data field
            enable = formData.getBoolean("enableLinenumber");
 
            // 儲存資料於硬碟中
            save();
            return super.configure(req,formData);
        }

        /**
         * 覆寫getDisplayName，回傳的字串將顯示於Jenkins global configuration設定頁面
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "Enable Line number in Console log";
        }

        /**
         * get方法，global.jelly透過getEnableLinenumber取得是否啟用line number設定
         */
        public boolean getEnableLinenumber() {
            return enable;
        }  
    }
}

