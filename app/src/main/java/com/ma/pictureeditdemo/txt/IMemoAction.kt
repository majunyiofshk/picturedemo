package com.ma.pictureeditdemo.txt

/*
* 每个图层实现该接口
* */
interface IMemoAction<T : IMemo> {
    fun save(memo: T)
    fun restore(memo: T)
}