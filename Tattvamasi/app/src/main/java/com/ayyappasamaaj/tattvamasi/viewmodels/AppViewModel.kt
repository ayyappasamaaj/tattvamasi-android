package com.ayyappasamaaj.tattvamasi.viewmodels

import androidx.lifecycle.ViewModel
import com.ayyappasamaaj.tattvamasi.model.GridItem

/**
 * Created by Gangadhar Kondati on 31,March,2022
 */
class AppViewModel: ViewModel() {

    val bhajanItemsList = ArrayList<GridItem>().apply {
        add(GridItem("Ganesha"))
        add(GridItem("Guru"))
        add(GridItem("Muruga"))
        add(GridItem("Devi"))
        add(GridItem("Shiva"))
        add(GridItem("Vishnu"))
        add(GridItem("Rama"))
        add(GridItem("Hanuman"))
        add(GridItem("Ayyappan"))
    }

    val bhajansList = ArrayList<GridItem>().apply {
        add(GridItem("Bhajans"))
        add(GridItem("Pooja"))
        add(GridItem("Articles"))
        add(GridItem("Events"))
        add(GridItem("Donate"))
        add(GridItem("About"))
    }

}