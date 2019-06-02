//
// Created by jieyaozu on 2019/6/2.
//

#ifndef VIDEOEDITTEST_SHOWFILTER_H
#define VIDEOEDITTEST_SHOWFILTER_H

#include "OESFilter.h"

class ShowFilter : public OESFilter {
public:
    ShowFilter();

    virtual ~ShowFilter();

    void initProgram() override;

    void setVideoSize(int videoWidth, int videoHeight) override;

protected:
    //离屏
    unsigned int FBUFFERS[1];
    unsigned int FBUFFERTEXTURE[1];
};

#endif //VIDEOEDITTEST_SHOWFILTER_H
