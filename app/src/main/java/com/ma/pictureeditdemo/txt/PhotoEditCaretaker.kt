package com.ma.pictureeditdemo.txt

class PhotoEditCaretaker<T : IMemo> {
    private val mMemos = mutableListOf<T>()
    private var mIndex = -1
    
    fun saveMemo(memo: T){
        if (mIndex < mMemos.size - 1){
            // 说明做了回退操作,删除后面部分
            mMemos.subList(mIndex + 1, mMemos.size).clear()
        }
        mMemos.add(memo)
        mIndex = mMemos.size - 1
    }
    
    // 获取上一步操作
    fun getPrevMemo(): T{
        mIndex = if (mIndex > 0) --mIndex else mIndex
        return mMemos[mIndex]
    }
    
    //获取下一步操作
    fun getNextMemo(): T{
        mIndex = if (mIndex < mMemos.size - 1) ++mIndex else mIndex
        return mMemos[mIndex]
    }
}