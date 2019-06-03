//
// Created by jieyaozu on 2019/6/3.
//

#ifndef VIDEOEDITTEST_BLURFILTER_H
#define VIDEOEDITTEST_BLURFILTER_H

#include "OESFilter.h"

class BlurFilter : public OESFilter {
public:
    BlurFilter();

    virtual ~BlurFilter();

    void initProgram() override;

    void setVideoSize(int videoWidth, int videoHeight) override;

    int drawFrameBuffer(int textureId) override;

protected:

    virtual void destroyFrameBuffer() override ;

    virtual void genFrameTexturesBuffer() override ;
protected:
    int texelWidthOffsetHandle;
    int texelHeightOffsetHandle;
    bool isVertical = true;
    //离屏
    unsigned int FBUFFERS[1];
    unsigned int FBUFFERTEXTURE[2];
private:
    int index = 0;
};

#endif //VIDEOEDITTEST_BLURFILTER_H
