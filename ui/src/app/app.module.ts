import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { AppComponent } from './app.component';
import {
  HomeComponent, 
  NavbarComponent,
  FooterComponent,
  UploadComponent,
  TorrentDetailComponent,
  SearchComponent,
  ExportComponent } from './components';

import {
  MomentPipe,
  FileSizePipe } from './pipes';

import {
  SearchService,
  TorrentDetailService } from './services';

import { AppRoutingModule }     from './app-routing.module';
import { PaginationModule } from 'ng2-bootstrap/ng2-bootstrap';
import { FileUploadModule } from 'ng2-file-upload/ng2-file-upload';

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    NavbarComponent,
    FooterComponent,
    SearchComponent,
    UploadComponent,
    FileSizePipe,
    MomentPipe,
    TorrentDetailComponent,
    ExportComponent,
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    AppRoutingModule,
    PaginationModule,
    FileUploadModule
  ],
  providers: [SearchService, TorrentDetailService],
  bootstrap: [AppComponent]
})
export class AppModule { }
