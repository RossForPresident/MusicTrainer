//
// Created by Ross Wilhite on 4/2/2017.
//
#include <stdint.h>
#include <stdlib.h>
#include <AndroidIO/Yin.h>



void Difference(Yin* yin, int16_t* buff){
    int16_t i;
    int16_t tau;
    float delta;

   /* for (i = yin->mintau; i < yin->bufferSize; i++){
        for (tau = yin->mintau; tau <= i && tau <= yin->halfBufferSize; tau++){
            delta = buff[i - tau] - buff[i];
            yin->yinBuffer2[tau] += delta * delta;
        }

    }*/


    for (tau = yin->mintau; tau < yin->halfBufferSize; tau++){



        for(i = 0; i < yin->halfBufferSize; i++){
            delta = buff[i] - buff[i + tau];
            yin->yinBuffer[tau] += delta * delta;
        }
    }

}


void Meannormalized(Yin *yin){
    int16_t tau;
    float Sum = 0;
    for (int t = 0; t < (int)yin->Fs/yin->maxHz; t++){
    yin->yinBuffer[t] = 1;}


    /* Sum all the values in the autocorellation buffer and nomalise the result, replacing
     * the value in the autocorellation buffer with a cumulative mean of the normalised difference */
    for (tau = 1; tau < yin->halfBufferSize; tau++){
        Sum += yin->yinBuffer[tau];
        yin->yinBuffer[tau] *= tau / Sum;
    }
}

int16_t absThreshold(Yin *yin){
    int16_t tau;

    /* Search through the array of cumulative mean values, and look for ones that are over the threshold
     * The first two positions in yinBuffer are always so start at the third (index 2) */
    for (tau = 2; tau < yin->halfBufferSize ; tau++) {
        if (yin->yinBuffer[tau] < yin->thresh) {
            while (tau + 1 < yin->halfBufferSize && yin->yinBuffer[tau + 1] < yin->yinBuffer[tau]) {
                tau++;
            }
            /* found tau, exit loop and return
             * store the probability
             * From the YIN paper: The yin->threshold determines the list of
             * candidates admitted to the set, and can be interpreted as the
             * proportion of aperiodic power tolerated
             * within a periodic signal.
             *
             * Since we want the periodicity and and not aperiodicity:
             * periodicity = 1 - aperiodicity */
            yin->probability = 1 - yin->yinBuffer[tau];
            break;
        }
    }

    /* if no pitch found, tau => -1 */
    if (tau == yin->halfBufferSize || yin->yinBuffer[tau] >= yin->thresh) {
        tau = -1;
        yin->probability = 0;
    }

    return tau;
}
/**
 * Step 5: Interpolate the shift value (tau) to improve the pitch estimate.
 * @param  yin         [description]
 * @param  tauEstimate [description]
 * @return             [description]
 *
 * The 'best' shift value for autocorellation is most likely not an interger shift of the signal.
 * As we only autocorellated using integer shifts we should check that there isn't a better fractional
 * shift value.
 */
float Yin_parabolicInterpolation(Yin *yin, int16_t tauEstimate) {
    float betterTau;
    int16_t x0;
    int16_t x2;

    /* Calculate the first polynomial coeffcient based on the current estimate of tau */
    if (tauEstimate < 1) {
        x0 = tauEstimate;
    }
    else {
        x0 = tauEstimate - 1;
    }

    /* Calculate the second polynomial coeffcient based on the current estimate of tau */
    if (tauEstimate + 1 < yin->halfBufferSize) {
        x2 = tauEstimate + 1;
    }
    else {
        x2 = tauEstimate;
    }

    /* Algorithm to parabolically interpolate the shift value tau to find a better estimate */
    if (x0 == tauEstimate) {
        if (yin->yinBuffer[tauEstimate] <= yin->yinBuffer[x2]) {
            betterTau = tauEstimate;
        }
        else {
            betterTau = x2;
        }
    }
    else if (x2 == tauEstimate) {
        if (yin->yinBuffer[tauEstimate] <= yin->yinBuffer[x0]) {
            betterTau = tauEstimate;
        }
        else {
            betterTau = x0;
        }
    }
    else {
        float s0, s1, s2;
        s0 = yin->yinBuffer[x0];
        s1 = yin->yinBuffer[tauEstimate];
        s2 = yin->yinBuffer[x2];
        // fixed AUBIO implementation, thanks to Karl Helgason:
        // (2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
        betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
    }


    return betterTau;
}

/**
 * Initialise the Yin pitch detection object
 * @param yin        Yin pitch detection object to initialise
 * @param bufferSize Length of the audio buffer to analyse
 * @param threshold  Allowed uncertainty (e.g 0.05 will return a pitch with ~95% probability)
 */
void setYin(Yin *yin, int Sampler, int16_t buffers, float thr){
//void Yin_init(Yin *yin, int16_t bufferSize, float threshold){
    /* Initialise the fields of the Yin structure passed in */
    yin->bufferSize = buffers;
    yin->halfBufferSize = buffers / 2;
    yin->probability = 0.0;
    yin->thresh = thr;
    yin->Fs = Sampler;

    /* Allocate the autocorellation buffer and initialise it to zero */
    yin->yinBuffer = (float *) malloc(sizeof(float)* yin->halfBufferSize);
    yin->yinBuffer2 = (float *) malloc(sizeof(float)* yin->halfBufferSize);

    int16_t i;
    for(i = 0; i < yin->halfBufferSize; i++){
        yin->yinBuffer[i] = 0;
        yin->yinBuffer2[i] = 0;
    }
}
/**
 * Runs the Yin pitch detection algorithm
 * @param  yin    Initialised Yin object
 * @param  buffer Buffer of samples to analyse
 * @param  HzHi Max frequency expected
 * @return        Fundamental frequency of the signal in Hz. Returns -1 if pitch can't be found
 */
float YinPitch(Yin *yin, int16_t* buffer, int16_t HzHi){
    int16_t tauEstimate = -1;
    float pitchInHertz = -1;
    yin->maxHz = HzHi;
    yin->mintau = (yin->Fs) / (yin->maxHz);

    /* Step 1: Calculates the squared difference of the signal with a shifted version of itself. */
    Difference(yin, buffer);

    /* Step 2: Calculate the cumulative mean on the normalised difference calculated in step 1 */
    Meannormalized(yin);

    /* Step 3: Search through the normalised cumulative mean array and find values that are over the threshold */
    tauEstimate = absThreshold(yin);

    /* Step 5: Interpolate the shift value (tau) to improve the pitch estimate. */
    if(tauEstimate != -1){
        pitchInHertz = yin->Fs / Yin_parabolicInterpolation(yin, tauEstimate);
    }

    return pitchInHertz;
}

/**
 * Certainty of the pitch found
 * @param  yin Yin object that has been run over a buffer
 * @return     Returns the certainty of the note found as a decimal (i.e 0.3 is 30%)
 */
float Yin_getProbability(Yin *yin){
    return yin->probability;
}
