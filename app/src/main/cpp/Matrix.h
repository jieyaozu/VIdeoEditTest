//
// Created by jieyaozu on 2019/6/2.
//

#ifndef VIDEOEDITTEST_MATRIX_H
#define VIDEOEDITTEST_MATRIX_H

void orthoM(float m[], int mOffset,
            float left, float right, float bottom, float top,
            float near, float far);

void scaleM(float m[], int mOffset,
       float x, float y, float z);

void flip(float m[], bool x, bool y);

#endif //VIDEOEDITTEST_MATRIX_H
